package org.servalproject.networking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

public class WifiNetworkChanges extends BroadcastReceiver {
	public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";

	public WifiNetworkChanges() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
			// TODO
		} else if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
			// TODO force network connections in the background?
		} else if (action.equals(WIFI_AP_STATE_CHANGED_ACTION)) {
			// TODO
		} else if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
			// TODO
		} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			// TODO
		}
	}
}
