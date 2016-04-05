package org.servalproject.servalchat.networking;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothNetworkChanges extends BroadcastReceiver {
	public BluetoothNetworkChanges() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(BluetoothDevice.ACTION_FOUND)) {
			// TODO
		} else if (action.equals(BluetoothDevice.ACTION_NAME_CHANGED)) {
			// TODO
		} else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
			// TODO
		} else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
			// TODO
		} else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
			// TODO
		} else if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
			// TODO
		} else if (action.equals(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {
			// TODO
		}
	}
}
