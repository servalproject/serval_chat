package org.servalproject.mid.networking.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import org.servalproject.mid.networking.NetworkInfo;

public class Scanner{
	private final BlueToothControl control;
	private final BluetoothAdapter adapter;
	private int scanMode;
	private long lastScanStart=0;
	private long lastPeerScanned=0;
	private long lastScanEnd=1;

	private static final String TAG = "BTScanner";

	Scanner(BlueToothControl control, BluetoothAdapter adapter){
		this.control = control;
		this.adapter = adapter;
		setState();
	}

	private void setState(int newMode){
		if (scanMode == newMode)
			return;
		scanMode = newMode;
		Log.v(TAG, "Scan mode changed; " + scanMode + " " + adapter.isEnabled());
		lastScanStart=0;
		lastPeerScanned=0;
		lastScanEnd=1;
		if (adapter.isEnabled() && adapter.isDiscovering())
			lastScanStart = SystemClock.elapsedRealtime();
	}

	void setState(){
		setState(adapter.getScanMode());
	}

	public void onScanModeChanged(Intent intent) {
		setState(intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, 0));
	}

	public void onDiscoveryStarted() {
		lastScanStart = SystemClock.elapsedRealtime();
		Log.v(TAG, "Discovery Started");
		// TODO set alarm to cancel / restart bluetooth
	}

	public void onDiscoveryFinished() {
		lastScanEnd = SystemClock.elapsedRealtime();
		Log.v(TAG, "Discovery finished");
		control.runNext();
	}

	public void onPeerScanned(){
		lastPeerScanned = SystemClock.elapsedRealtime();
	}

	public boolean isDiscovering() {
		return adapter.isDiscovering();
	}

	public void startDiscovery() {
		if (control.networkInfo.getState() != NetworkInfo.State.On || !adapter.isEnabled())
			return;
		if (adapter.startDiscovery())
			lastScanStart = SystemClock.elapsedRealtime();
	}

	private static final int RESCAN_INTERVAL = 30000;
	private static final int SCAN_IDLE_TIME = 5000;
	private static final int CANCEL_BROKEN = 10000;
	private static final int POLL_INTERVAL = 1000;

	public int nextScanAction(Connector item){
		long now = SystemClock.elapsedRealtime();

		if (lastScanStart < lastScanEnd){
			if (item!=null) {
				item.moveNext();
				return POLL_INTERVAL;
			}

			long nextScanDelay = lastScanEnd + RESCAN_INTERVAL - now;
			if (nextScanDelay <=0) {
				startDiscovery();
				return POLL_INTERVAL;
			}
			return (int) nextScanDelay;
		}

		int delay = (item == null) ? RESCAN_INTERVAL : SCAN_IDLE_TIME;

		long cancelAfter = Math.max(lastScanStart, lastPeerScanned) + delay - now;
		if (cancelAfter <=0){
			cancelDiscovery();
			if (cancelAfter <= - CANCEL_BROKEN && cancelAfter >= - RESCAN_INTERVAL){
				if (item == null)
					startDiscovery();
				else {
					item.moveNext();
				}
			}
			return POLL_INTERVAL;
		}
		return (int) cancelAfter;
	}

	public void cancelDiscovery(){
		if (control.networkInfo.getState() != NetworkInfo.State.On || !adapter.isEnabled())
			return;
		if (adapter.isDiscovering())
			adapter.cancelDiscovery();
	}
}
