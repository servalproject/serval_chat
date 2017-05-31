package org.servalproject.mid.networking;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by jeremy on 31/05/17.
 */

public class FlightModeObserver extends ContentObserver {
	private final Networks networks;
	private final ContentResolver resolver;
	boolean flightMode;
	String flightModeRadios = "";
	String flightModeToggleable= "";

	private static final String TAG = "FlightMode";

	public FlightModeObserver(Networks networks, ContentResolver resolver, Handler handler) {
		super(handler);
		this.networks = networks;
		this.resolver = resolver;
	}

	void register(){
		if (Build.VERSION.SDK_INT >= 17) {
			resolver.registerContentObserver(Settings.Global.CONTENT_URI, true, this);
		}else {
			resolver.registerContentObserver(Settings.System.CONTENT_URI, true, this);
		}
		onChange(false, null);
	}

	void unregister(){
		resolver.unregisterContentObserver(this);
	}

	@Override
	public void onChange(boolean selfChange) {
		onChange(selfChange, null);
	}

	@Override
	public void onChange(boolean selfChange, Uri uri) {
		boolean airplaneMode;
		String airplaneRadios;
		String airplaneToggleable;

		try {
			if (Build.VERSION.SDK_INT >= 17){
				airplaneMode = Settings.Global.getInt(resolver, Settings.Global.AIRPLANE_MODE_ON) !=0;
				airplaneRadios = Settings.Global.getString(resolver, Settings.Global.AIRPLANE_MODE_RADIOS);
				airplaneToggleable= Settings.Global.getString(resolver, "airplane_mode_toggleable_radios");
			}else {
				airplaneMode = Settings.System.getInt(resolver, Settings.System.AIRPLANE_MODE_ON) !=0;
				airplaneRadios = Settings.System.getString(resolver, Settings.System.AIRPLANE_MODE_RADIOS);
				airplaneToggleable = Settings.System.getString(resolver, "airplane_mode_toggleable_radios");
			}
		} catch (Settings.SettingNotFoundException e) {
			throw new IllegalStateException(e);
		}

		Log.v(TAG, airplaneMode+", ["+airplaneRadios+"], ["+airplaneToggleable+"]");

		if (airplaneMode == this.flightMode
				&& this.flightModeRadios.equals(airplaneRadios)
		&& this.flightModeToggleable.equals(airplaneToggleable))
			return;

		this.flightMode = airplaneMode;
		this.flightModeRadios = airplaneRadios;
		this.flightModeToggleable = airplaneToggleable;
		networks.onFlightModeChanged();
	}
}