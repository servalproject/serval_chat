package org.servalproject.mid;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import org.servalproject.servaldna.IJniServer;
import org.servalproject.servaldna.ServalDClient;
import org.servalproject.servaldna.ServalDCommand;

/**
 * Created by jeremy on 3/05/16.
 */
public class Server extends BroadcastReceiver implements IJniServer, Runnable {
	private static final String TAG = "Server";

	Server(Serval serval, Context context) {
		this.context = context;
		this.serval = serval;
		am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Services");
		observers = new ObserverSet<>(serval.uiHandler, this);
	}

	/*
		If the android CPU suspends,
		the poll timeout that the daemon thread uses to run the next scheduled job will stop counting down
		So we schedule an alarm to fire just after the next job should run.
		If the CPU has not suspended, poll should return normally.
		If the alarm does fire, we know the CPU suspended. So we signal the other thread to wake up and
		for poll to return EINTR.
	 */
	private final Serval serval;
	private final Context context;
	private final PowerManager.WakeLock cpuLock;
	private final AlarmManager am;
	public final ObserverSet<Server> observers;

	private long wakeAt;
	private PendingIntent alarmIntent = null;
	private long alarmTime;
	private int serverTid = 0;

	private static final int MIN_WAIT = 10;
	private static final String WAKE_INTENT = "org.servalproject.WAKE";
	private static final int SIGIO = 29;

	@Override
	public long aboutToWait(long now, long nextRun, long nextWake) {
		// Release android lock and set an alarm
		// Note that we may need to allow this thread to enter poll() first
		// (all times are using the same clock as System.currentTimeMillis())
		synchronized (this) {
			this.wakeAt = SystemClock.elapsedRealtime() + (nextWake - now);
			serval.backgroundHandler.replaceMessage(Serval.CPU_LOCK, 1);
		}
		return nextWake;
	}

	// called from event handler thread
	void onCpuLock() {
		synchronized (this) {
			long delay = this.wakeAt - SystemClock.elapsedRealtime();

			if (delay > MIN_WAIT) {
				if (alarmIntent == null || alarmTime != wakeAt) {
					alarmIntent = PendingIntent.getBroadcast(
							context,
							0,
							new Intent(WAKE_INTENT),
							PendingIntent.FLAG_UPDATE_CURRENT);
					am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, wakeAt, alarmIntent);
					alarmTime = wakeAt;
				}

				if (cpuLock.isHeld())
					cpuLock.release();
			} else {
				// stay awake
				if (!cpuLock.isHeld())
					cpuLock.acquire();
				if (alarmIntent != null) {
					am.cancel(alarmIntent);
					alarmIntent = null;
				}
			}
		}
		if (alarmIntent != null) {
			am.cancel(alarmIntent);
			alarmIntent = null;
		}
	}

	@Override
	public void wokeUp() {
		// the server returned from poll, stake awake!
		synchronized (this) {
			wakeAt = 0;
			if (!cpuLock.isHeld())
				cpuLock.acquire();
			if (alarmIntent != null) {
				am.cancel(alarmIntent);
				alarmIntent = null;
			}
		}
	}

	private int mdpPort =-1;
	public int getMdpPort(){
		return mdpPort;
	}

	private int httpPort;
	public int getHttpPort(){
		return httpPort;
	}

	public boolean isRunning(){
		return mdpPort>0;
	}

	@Override
	public void started(String instancePath, int pid, int mdpPort, int httpPort) {
		this.mdpPort = mdpPort;
		this.httpPort = httpPort;
		// don't let observers block this thread!
		serval.backgroundHandler.replaceMessage(Serval.SERVER_UP, 0);
	}

	void onStart(){
		IntentFilter filter = new IntentFilter();
		filter.addAction(WAKE_INTENT);
		context.registerReceiver(this, filter);
		observers.onUpdate();
	}

	@Override
	public void run() {
		cpuLock.acquire();
		serverTid = android.os.Process.myTid();
		wakeAt = 0;

		Log.v(TAG, "Calling native method server()");
		ServalDCommand.server(this, "", null);

		// we don't currently stop the server, so this is effectively unreachable
		throw new IllegalStateException("Server failed to start");
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(WAKE_INTENT)) {
			// This should only occur if the CPU has suspended and we need to interrupt poll.
			synchronized (this) {
				alarmIntent = null;
				if (!cpuLock.isHeld())
					cpuLock.acquire();
				if (wakeAt != 0)
					android.os.Process.sendSignal(serverTid, SIGIO);
			}
		}
	}

	@Override
	public String toString() {
		return "Server{" +
				"serverTid=" + serverTid +
				", mdpPort=" + mdpPort +
				", httpPort=" + httpPort +
				'}';
	}
}
