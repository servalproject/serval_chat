/**
 * Copyright (C) 2011 The Serval Project
 *
 * This file is part of Serval Software (http://www.servalproject.org)
 *
 * Serval Software is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.servalproject.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.os.Process;
import android.util.Log;

import org.servalproject.servaldna.AsyncResult;
import org.servalproject.servaldna.ChannelSelector;
import org.servalproject.servaldna.IJniServer;
import org.servalproject.servaldna.MdpDnaLookup;
import org.servalproject.servaldna.MdpServiceLookup;
import org.servalproject.servaldna.ServalDCommand;
import org.servalproject.servaldna.ServalDFailureException;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.ServerControl;
import org.servalproject.servaldna.keyring.KeyringIdentity;
import org.servalproject.servaldna.keyring.KeyringIdentityList;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Low-level class for invoking servald JNI command-line operations.
 *
 * @author Andrew Bettison <andrew@servalproject.com>
 */
public class ServalD implements IJniServer
{
	private static final String TAG = "ServalD";

	private final Context context;
	private final ServerControl server;
	private ChannelSelector selector;
	private KeyringIdentity identity = null;

	private static ServalD instance;

	private ServalD(Context context){
		server = new ServerControl(null);
		this.context = context;
		Log.i(TAG, "Starting servald background thread");
		serverThread=new Thread(this.runServer, "Servald");
		serverThread.start();
	}

	private ChannelSelector getChannel() throws IOException {
		if (selector==null)
			selector = new ChannelSelector();
		return selector;
	}

	public static synchronized ServalD startServer(Context context) throws ServalDFailureException {
		if (instance==null)
			instance = new ServalD(context.getApplicationContext());
		return instance;
	}

	public synchronized KeyringIdentity getIdentity() throws ServalDInterfaceException, IOException {
		if (identity==null){
			KeyringIdentityList list = server.getRestfulClient().keyringListIdentities(null);
			identity = list.nextIdentity();
		}
		return identity;
	}

	public synchronized KeyringIdentity setIdentityDetails(KeyringIdentity identity, String did, String name) throws ServalDInterfaceException, IOException {
		this.identity = server.getRestfulClient().keyringSetDidName(identity.sid, did, name, null);
		return this.identity;
	}

	public MdpServiceLookup getMdpServiceLookup(AsyncResult<MdpServiceLookup.ServiceResult> results) throws ServalDInterfaceException, IOException {
		return server.getMdpServiceLookup(getChannel(), results);
	}

	public MdpDnaLookup getMdpDnaLookup(AsyncResult<ServalDCommand.LookupResult> results) throws ServalDInterfaceException, IOException {
		return server.getMdpDnaLookup(getChannel(), results);
	}

	private File rhizomePath = null;
	public boolean isRhizomeEnabled() {
		return rhizomePath!=null;
	}

/*
	If the android CPU suspends,
	the poll timeout that the daemon thread uses to run the next scheduled job will stop counting down
	So we schedule an alarm to fire just after the next job should run.
	If the CPU has not suspended, poll should return normally.
	If the alarm does fire, we know the CPU suspended. So we signal the other thread to wake up and
	for poll to return EINTR.
 */

	private PowerManager.WakeLock cpuLock;
	private volatile long wakeAt; // >0, wait until this SystemClock.elapsedRealtime(), 0 = running, -1 infinite wait.
	private AlarmManager am;

	@Override
	public long aboutToWait(long now, long nextRun, long nextWake) {
		// Release android lock and set an alarm
		// Note that we may need to allow this thread to enter poll() first
		// (all times are using the same clock as System.currentTimeMillis())
		long delay = (nextWake - now);
		synchronized (receiver) {
			if (delay>=100){
				// set an alarm for 10ms after the daemon wants to wake up.
				// if the CPU suspends the poll timeout will not elapse

				// more than one day? might as well be infinite!
				if (delay > 1000*60*60*24)
					wakeAt = -1;
				else
					wakeAt = SystemClock.elapsedRealtime() + delay + 100;

				Background.Low.run(releaseLock, 1);
			}else{
				wakeAt = 0;
				if(alarmIntent !=null) {
					am.cancel(alarmIntent);
					alarmIntent = null;
				}
			}
		}

		return nextWake;
	}

	@Override
	public void wokeUp() {
		// hold wakelock until the next call to aboutToWait
		synchronized (receiver) {
			wakeAt = 0;
			if (!cpuLock.isHeld()) {
				cpuLock.acquire();
			}
			if(alarmIntent !=null) {
				am.cancel(alarmIntent);
				alarmIntent = null;
			}
		}
	}

	private File getRhizomePath(){
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File folder = context.getExternalFilesDir(null);
			if (folder != null) {
				return new File(folder, "rhizome");
			}
		}
		return null;
	}

	private void buildRhizomeConfig(List<Object> configChanges, File rhizomeFolder){
		if (rhizomeFolder == null) {
			set(configChanges, "rhizome.enable", "0");
		}else{
			set(configChanges, "rhizome.enable", "1");
			set(configChanges, "rhizome.datastore_path", rhizomeFolder.getPath());
		}
	}

	private void setRhizomeConfig() throws ServalDFailureException {
		File rhizomeFolder = getRhizomePath();

		if (rhizomeFolder == null && this.rhizomePath==null)
			return;
		if (rhizomeFolder != null && this.rhizomePath!=null && rhizomeFolder.equals(rhizomePath))
			return;

		this.rhizomePath = rhizomeFolder;
		List<Object> configChanges = new ArrayList<Object>();
		buildRhizomeConfig(configChanges, rhizomeFolder);
		if (configChanges.size()>0) {
			configChanges.add(ServalDCommand.ConfigAction.sync);
			ServalDCommand.configActions(configChanges.toArray(new Object[configChanges.size()]));
		}
	}

	@Override
	public void started(String instancePath, int pid, int mdpPort, int httpPort) {
		server.setStatus(instancePath, pid, mdpPort, httpPort);
		IntentFilter filter = new IntentFilter();
		filter.addAction(WAKE_INTENT);

		// detect when the sdcard is mounted
		filter.addAction(Intent.ACTION_MEDIA_EJECT);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);

		context.registerReceiver(receiver, filter);
		Log.i(TAG, "Server started");

		Background.Low.run(new Runnable() {
			@Override
			public void run() {
				// TODO post init tasks....?
			}
		});
	}

	private static final String WAKE_INTENT = "org.servalproject.WAKE";
	private static final int SIGIO=29;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				String action = intent.getAction();
				if (action.equals(WAKE_INTENT)) {
					// This should only occur if the CPU has suspended and we need to interrupt poll.
					alarmIntent = null;
					synchronized (receiver) {
						if (cpuLock!= null && !cpuLock.isHeld()) {
							cpuLock.acquire();
						}
						if (wakeAt!=0) {
							android.os.Process.sendSignal(serverTid, SIGIO);
						}
					}

				}else if(action.equals(Intent.ACTION_MEDIA_EJECT) ||
						action.equals(Intent.ACTION_MEDIA_UNMOUNTED) ||
						action.equals(Intent.ACTION_MEDIA_MOUNTED)){
					// redetect sdcard path & availability
					setRhizomeConfig();
				}
			} catch (ServalDFailureException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
	};

	private PendingIntent alarmIntent = null;

	private Runnable releaseLock = new Runnable(){
		@Override
		public void run() {
			PowerManager.WakeLock lock = cpuLock;
			Intent intent = new Intent(WAKE_INTENT);
			PendingIntent pe = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			synchronized (receiver){
				// last moment check that it is safe to release the lock
				if (lock != null && wakeAt>0) {
					alarmIntent = pe;
					am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, wakeAt, pe);
				}else if(alarmIntent !=null){
					am.cancel(alarmIntent);
					alarmIntent = null;
				}

				if (wakeAt!=0 && cpuLock.isHeld())
					cpuLock.release();
			}
		}
	};

	private Thread serverThread=null;
	private int serverTid=0;

	private void set(List<Object> configChanges, String option, String value){
		configChanges.add(ServalDCommand.ConfigAction.set);
		configChanges.add(option);
		configChanges.add(value);
	}

	private Runnable runServer = new Runnable() {
		@Override
		public void run() {
			serverTid = Process.myTid();
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			if (am==null)
				am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

			cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Services");

			cpuLock.acquire();

			try {
				File appFolder = context.getFilesDir().getParentFile();
				ServalDCommand.setInstancePath(new File(appFolder,"instance").getPath());

				List<Object> configChanges = new ArrayList<Object>();

				// if sdcard is available, enable rhizome
				rhizomePath = getRhizomePath();
				buildRhizomeConfig(configChanges, rhizomePath);

				// roll a new restful api password, partly so we only parse config once on the critical path for startup
				// partly for slightly better security
				String restfulPassword = new BigInteger(130, new SecureRandom()).toString(32);
				set(configChanges, "api.restful.users." + ServerControl.restfulUsername + ".password", restfulPassword);
				set(configChanges, "interfaces.0.match", "eth0,tiwlan0,wlan0,wl0.1,tiap0");
				set(configChanges, "interfaces.0.default_route", "on");
				set(configChanges, "mdp.enable_inet", "on");

				ServalDCommand.configActions(configChanges.toArray(new Object[configChanges.size()]));

				// TODO if debuggable, set log path to SDcard?

			} catch (ServalDFailureException e) {
				Log.v(TAG, e.getMessage(), e);
			}

			Log.v(TAG, "Calling native method server()");
			ServalDCommand.server(ServalD.this, "", null);

			// we don't currently stop the server, so this is effectively unreachable
			throw new IllegalStateException("Server failed to start");

		}
	};
}
