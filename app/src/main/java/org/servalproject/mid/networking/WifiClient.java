package org.servalproject.mid.networking;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;

/**
 * Created by jeremy on 2/11/16.
 */
public class WifiClient extends NetworkInfo{
	private WifiManager manager;
	private static final String TAG = "WifiClient";

	protected WifiClient(Serval serval) {
		super(serval);
		manager = (WifiManager) serval.context.getSystemService(Context.WIFI_SERVICE);
		setState(statusToState(manager.getWifiState()));
	}

	@Override
	public String getName(Context context) {
		return context.getString(R.string.wifi_client);
	}

	@Override
	public String getStatus(Context context) {
		if (getState()==State.Off && serval.networks.getGoal() == Networks.WifiGoal.ClientOn)
			return context.getString(R.string.queued);
		return super.getStatus(context);
	}

	void setEnabled(boolean enabled){
		manager.setWifiEnabled(enabled);
	}

	@Override
	public void enable(Context context) {
		serval.networks.setWifiGoal(Networks.WifiGoal.ClientOn);
	}

	@Override
	public void disable(Context context) {
		serval.networks.setWifiGoal(Networks.WifiGoal.Off);
	}

	public static State statusToState(int state) {
		switch (state) {
			case WifiManager.WIFI_STATE_DISABLED:
				return State.Off;
			case WifiManager.WIFI_STATE_DISABLING:
				return State.Stopping;
			case WifiManager.WIFI_STATE_ENABLED:
				return State.On;
			case WifiManager.WIFI_STATE_ENABLING:
				return State.Starting;
			default:
				Log.v(TAG, "Unknown state: "+state);
				return State.Error;
		}
	}

	public void onStateChanged(Intent intent) {
		setState(statusToState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)));
	}
}
