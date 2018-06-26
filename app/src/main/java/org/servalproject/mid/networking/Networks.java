package org.servalproject.mid.networking;

import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.util.Log;

import org.servalproject.mid.ListObserverSet;
import org.servalproject.mid.Observer;
import org.servalproject.mid.Serval;
import org.servalproject.mid.Server;
import org.servalproject.mid.networking.bluetooth.BlueToothControl;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 2/11/16.
 */
public class Networks implements Observer<NetworkInfo> {

	public final List<NetworkInfo> networks = new ArrayList<>();
	private static final String TAG = "Networks";
	public BlueToothControl blueTooth;
	private final Serval serval;
	public final WifiClient wifiClient;
	public final Hotspot wifiHotspot;
	public WifiAware wifiAware;
	public final ListObserverSet<NetworkInfo> observers;
	final FlightModeObserver flightModeObserver;

	private Networks(Serval serval){
		this.serval = serval;
		observers = new ListObserverSet<>(serval);
		this.wifiClient = new WifiClient(serval);
		this.wifiHotspot = Hotspot.getHotspot(serval);

		this.flightModeObserver = new FlightModeObserver(
				this,
				serval.context.getContentResolver(),
				serval.backgroundHandler);

		serval.server.observers.addBackground(new Observer<Server>() {
			@Override
			public void updated(Server obj) {
				onStart();
			}
		});
	}

	private void onStart(){
		wifiClient.observers.addBackground(this);
		if (wifiHotspot!=null)
			wifiHotspot.observers.addBackground(this);

		blueTooth = BlueToothControl.getBlueToothControl(serval, serval.selector, serval.server.getMdpPort());
		if (blueTooth != null) {
			blueTooth.onEnableChanged();
			networks.add(blueTooth.networkInfo);
			blueTooth.networkInfo.observers.addBackground(this);
		}
		networks.add(wifiClient);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.wifiAware = WifiAware.getWifiAware(serval, serval.selector, serval.server.getMdpPort());
		}
		if (wifiHotspot!=null)
			networks.add(wifiHotspot);
		flightModeObserver.register();
	}

	private static Networks instance;
	public static void init(Serval serval){
		if (instance!=null)
			throw new IllegalStateException();
		instance = new Networks(serval);
	}

	public static Networks getInstance(){
		return instance;
	}

	public boolean canEnable(NetworkInfo networkInfo){
		if (!flightModeObserver.flightMode)
			return true;

		String name = networkInfo.getRadioName();
		if (name == null)
			return true;

		for (String allowed: flightModeObserver.flightModeToggleable.split(",")) {
			if (name.equals(allowed))
				return true;
		}

		return false;
	}

	void onFlightModeChanged(){
		// just force a full update
		observers.onReset();
	}

	@Override
	public void updated(NetworkInfo obj) {
		observers.onUpdate(obj);

		if (obj == wifiClient || obj == wifiHotspot) {
			// if the user fiddles with wifi && we need to fix hotspot config
			// make sure we try to finish what they started
			NetworkInfo.State state = obj.getState();
			WifiGoal newGoal = null;
			if (state == NetworkInfo.State.Stopping && goal == null) {
				newGoal = WifiGoal.Off;
			} else if (state == NetworkInfo.State.Starting) {
				if (obj == wifiClient)
					newGoal = WifiGoal.ClientOn;
				else if (obj == wifiHotspot && goal == null)
					newGoal = WifiGoal.HotspotOn;
			}
			if (newGoal != null && goal != newGoal)
				setWifiGoal(newGoal);
			else
				progressToGoal();
		}
	}

	// Wifi client & hotspot state are intertwined and depend on the same hardware.
	// If you enable one of them, android will turn off the other for you.
	// We could depend on that, but....

	// Android's security model for hotspot config is a little broken,
	// we can enable the hotspot && reconfigure it, but perhaps only if it is off.

	// So if we want to jump from hotspot with our config to client mode we need to;
	// - turn off the hotspot (&& wait for the hotspot to turn off)
	// - turn on the hotspot with the user's old config (&& wait)
	// - turn off the hotspot
	// - turn on wifi client.

	enum WifiGoal{
		ClientOn,
		HotspotOn,
		HotspotOnServalConfig, // Hotspot is on, with our AP name
		Off
	};

	private WifiGoal goal;

	WifiGoal getGoal(){
		return goal;
	}

	void setWifiGoal(WifiGoal goal){
		if (this.goal == goal)
			return;
		if (wifiHotspot == null
				&& (goal == WifiGoal.HotspotOn || goal == WifiGoal.HotspotOnServalConfig))
			throw new IllegalStateException();
		this.goal = goal;
		Log.v(TAG, "Changed goal to "+goal);
		this.observers.onReset();
		progressToGoal();
	}

	private boolean turnOffClient(){
		NetworkInfo.State state = wifiClient.getState();
		if (state == NetworkInfo.State.On) {
			Log.v(TAG, "Turning wifi off");
			wifiClient.setEnabled(false);
		}
		return (state != NetworkInfo.State.Off);
	}

	private boolean turnOffHotspot(){
		try {
			if (wifiHotspot == null)
				return false;

			boolean shouldRestore = wifiHotspot.saved!=null && wifiHotspot.isServalConfig();
			if (shouldRestore)
				wifiHotspot.restoring = true;

			NetworkInfo.State state = wifiHotspot.getState();
			if (state == NetworkInfo.State.On) {
				Log.v(TAG, "Turning hotspot off");
				wifiHotspot.setWifiApEnabled(null, false);
			}

			if (state == NetworkInfo.State.Off && shouldRestore) {
				if (wifiClient.getState() == NetworkInfo.State.On) {
					Log.v(TAG, "Turning wifi off so we can restore hotspot config");
					wifiClient.setEnabled(false);
				}
				if (wifiClient.getState() != NetworkInfo.State.Off)
					return true;
				Log.v(TAG, "Turning hotspot back on to restore config");
				wifiHotspot.setWifiApEnabled(wifiHotspot.saved, true);
				return true;
			}

			if (state != NetworkInfo.State.Off)
				return true;

		}catch (SecurityException e){
			Log.e(TAG, e.getMessage(), e);
		}

		wifiHotspot.restoring = false;
		return false;
	}

	private Runnable progress = new Runnable() {
		@Override
		public void run() {
			if (goal == null)
				return;

			try {
				switch (goal) {
					case Off:
						if (turnOffHotspot())
							return;
						if (turnOffClient())
							return;
						goal = null;
						break;

					case ClientOn:
						if (turnOffHotspot())
							return;

						NetworkInfo.State clientState = wifiClient.getState();
						if (clientState == NetworkInfo.State.Off) {
							wifiClient.setEnabled(true);
							Log.v(TAG, "Enabling wifi client");
						}
						if (clientState == NetworkInfo.State.On)
							goal = null;
						break;

					case HotspotOn:
					case HotspotOnServalConfig:
						if (wifiHotspot == null)
							throw new NullPointerException();

						if (turnOffClient())
							return;

						NetworkInfo.State state = wifiHotspot.getState();
						WifiConfiguration config = null;
						boolean goalIsServal = (goal == WifiGoal.HotspotOnServalConfig);
						switch (state) {
							case Starting:
							case Stopping:
							case Error:
								return;

							case Off:
								if (wifiHotspot.isServalConfig() != goalIsServal)
									config = goalIsServal ? wifiHotspot.servalConfiguration : wifiHotspot.saved;
								Log.v(TAG, "Enabling hotspot" + (config == null ? "" : " and changing config"));
								wifiHotspot.setWifiApEnabled(config, true);
								break;

							case On:
								if (wifiHotspot.isServalConfig() != goalIsServal) {
									Log.v(TAG, "Disabling hotspot to change config");
									wifiHotspot.setWifiApEnabled(null, false);
								} else {
									wifiHotspot.restoring = false;
									goal = null;
								}
								break;
						}

						break;
				}
			}catch (Exception e){
				Log.e(TAG, e.getMessage(), e);
				goal = null;
			}

			// TODO re-run ourself to keep trying??
		}
	};

	private void progressToGoal() {
		if (goal == null)
			return;

		serval.runOnThreadPool(progress);
	}
}
