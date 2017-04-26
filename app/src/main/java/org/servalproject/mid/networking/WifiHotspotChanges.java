package org.servalproject.mid.networking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.servalproject.mid.Serval;

/**
 * Created by jeremy on 2/11/16.
 */
public class WifiHotspotChanges extends BroadcastReceiver {
	public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";

	@Override
	public void onReceive(Context context, Intent intent) {
		Serval serval = Serval.getInstance();
		if (serval == null)
			return;
		String action = intent.getAction();
		Hotspot hotspot = Networks.getInstance().wifiHotspot;
		if (hotspot == null)
			return;

		if (action.equals(WIFI_AP_STATE_CHANGED_ACTION)) {
			hotspot.onStateChanged(intent);
		}
	}
}
