package org.servalproject.networking;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.servalproject.mid.Serval;

public class BluetoothNetworkChanges extends BroadcastReceiver {
	public BluetoothNetworkChanges() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Serval serval = Serval.getInstance();
		if (serval == null)
			return;
		BlueToothControl blueTooth = serval.blueTooth;
		if (blueTooth == null)
			return;
		String action = intent.getAction();
		if (action.equals(BluetoothDevice.ACTION_FOUND)) {
			blueTooth.onFound(intent);
		} else if (action.equals(BluetoothDevice.ACTION_NAME_CHANGED)) {
			blueTooth.onRemoteNameChanged(intent);
		} else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
			blueTooth.onDiscoveryStarted();
		} else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
			blueTooth.onDiscoveryFinished();
		} else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
			blueTooth.onStateChange(intent);
		} else if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
			blueTooth.onScanModeChanged(intent);
		} else if (action.equals(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {
			blueTooth.onNameChanged(intent);
		}
	}
}
