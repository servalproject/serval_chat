package org.servalproject.mid.networking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.servalproject.mid.Serval;
import org.servalproject.servalchat.BuildConfig;
import org.servalproject.servalchat.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by jeremy on 2/11/16.
 */
public class Hotspot extends NetworkInfo {

	private static final String TAG = "Hotspot";
	private static final String profileName = "saved_hotspot";

	WifiConfiguration saved;
	WifiConfiguration servalConfiguration;
	WifiConfiguration current;
	boolean restoring = false;

	private final WifiManager mgr;

	private static Method getWifiApState;
	private static Method isWifiApEnabled;
	private static Method setWifiApEnabled;
	private static Method getWifiApConfiguration;

	protected Hotspot(Serval serval) {
		super(serval);
		mgr = (WifiManager) serval.context.getSystemService(Context.WIFI_SERVICE);
		servalConfiguration = new WifiConfiguration();
		servalConfiguration.SSID = serval.context.getString(R.string.SSID);
		servalConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

		testUserConfig();
		if (saved == null)
			saved = readProfile(profileName);

		setState(getWifiApState());
	}

	public boolean isServalConfig(){
		return isEqual(current, servalConfiguration);
	}

	private void testUserConfig(){
		WifiConfiguration current = getWifiApConfiguration();
		if (current!=null && !isEqual(current, servalConfiguration)) {
			saveProfile(profileName, current);
			saved = current;
		}
		this.current = current;
	}

	public static Hotspot getHotspot(Serval serval){
		Class<?> cls=WifiManager.class;
		for (Method method:cls.getDeclaredMethods()){
			String methodName=method.getName();
			if (methodName.equals("getWifiApState")){
				getWifiApState=method;
			}else if (methodName.equals("isWifiApEnabled")){
				isWifiApEnabled=method;
			}else if (methodName.equals("setWifiApEnabled")){
				setWifiApEnabled=method;
			}else if (methodName.equals("getWifiApConfiguration")){
				getWifiApConfiguration=method;
			}
		}

		if (getWifiApState == null || isWifiApEnabled == null
					|| setWifiApEnabled == null || getWifiApConfiguration == null)
			return null;

		return new Hotspot(serval);
	}

	public WifiConfiguration getWifiApConfiguration(){
		try {
			return (WifiConfiguration) getWifiApConfiguration.invoke(mgr);
		} catch (IllegalAccessException e) {
			// shouldn't happen
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			// shouldn't happen
			throw new IllegalStateException(e);
		}
	}

	public boolean isWifiApEnabled(){
		try {
			return (Boolean) isWifiApEnabled.invoke(mgr);
		} catch (IllegalAccessException e) {
			// shouldn't happen
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			// shouldn't happen
			throw new IllegalStateException(e);
		}
	}

	public static State statusToState(int state) {
		// Android's internal state constants were changed some time before
		// version 4.0
		if (state >= 10)
			state -= 10;

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
				Log.v(TAG, "Unhandled state "+state);
				return State.Error;
		}
	}

	public State getWifiApState(){
		try {
			return statusToState((Integer) getWifiApState.invoke(mgr));
		} catch (IllegalAccessException e) {
			// shouldn't happen
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			// shouldn't happen
			throw new IllegalStateException(e);
		}
	}

	public static boolean isEqual(WifiConfiguration config1, WifiConfiguration config2){
		if (config1 == config2)
			return true;
		if (config1 == null || config2 == null)
			return false;
		return config1.SSID.equals(config2.SSID)
				&& getKeyType(config1) == getKeyType(config2);
	}

	public boolean setWifiApEnabled(WifiConfiguration config, boolean enabled){
		if (config!=null)
			testUserConfig();
		try {
			return (Boolean) setWifiApEnabled.invoke(mgr, config, enabled);
		} catch (IllegalAccessException e) {
			// shouldn't happen
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			// shouldn't happen?
			if (e.getCause()!=null)
				throw new IllegalStateException(e.getCause());
			throw new IllegalStateException(e);
		}
	}

	private WifiConfiguration readProfile(String name) {
		SharedPreferences prefs = serval.context.getSharedPreferences(name, 0);
		String SSID = prefs.getString("ssid", null);
		if (SSID == null)
			return null;

		// android's WifiApConfigStore.java only uses these three fields.
		WifiConfiguration newConfig = new WifiConfiguration();
		newConfig.SSID = SSID;
		int keyType = prefs.getInt("key_type", WifiConfiguration.KeyMgmt.NONE);
		for (int i = 0; i <8; i++) {
			if ((keyType & (1<<i))!=0)
				newConfig.allowedKeyManagement.set(i);
		}
		newConfig.preSharedKey = prefs.getString("key", null);
		return newConfig;
	}

	private static int getKeyType(WifiConfiguration config){
		int keyType = 0;
		for (int i = 0; i <8; i++) {
			if (config.allowedKeyManagement.get(i))
				keyType |= (1<<i);
		}
		return keyType;
	}

	private void saveProfile(String name, WifiConfiguration config) {
		SharedPreferences prefs = serval.context.getSharedPreferences(name, 0);
		SharedPreferences.Editor ed = prefs.edit();
		ed.putString("ssid", config.SSID);
		ed.putInt("key_type", getKeyType(config));
		ed.putString("key", config.preSharedKey);
		ed.apply();
	}

	void onStateChanged(Intent intent) {
		testUserConfig();
		setState(statusToState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)));
	}

	@Override
	public String getName(Context context) {
		return context.getString(R.string.hotspot);
	}

	@Override
	public String getStatus(Context context) {
		if (restoring)
			return context.getString(R.string.restore_hotspot);

		switch (getState()){
			case Off:
				Networks.WifiGoal goal = Networks.getInstance().getGoal();
				if (goal == Networks.WifiGoal.HotspotOn || goal == Networks.WifiGoal.HotspotOnServalConfig)
					return context.getString(R.string.queued);
				break;

			case On:
				return context.getString(
						(getKeyType(current) & ~1) == 0 ?
						R.string.hotspot_open : R.string.hotspot_closed,
						current.SSID);
		}

		return super.getStatus(context);
	}

	@Override
	public void enable(Context context) {
		if (Build.VERSION.SDK_INT >= 23){
			if (!Settings.System.canWrite(context)){
				Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
						Uri.parse("package:" + BuildConfig.APPLICATION_ID));
				context.startActivity(i);
				return;
			}
		}
		boolean useConfig = serval.settings.getBoolean("hotspot_serval_config", true);
		Networks.getInstance().setWifiGoal(useConfig ? Networks.WifiGoal.HotspotOnServalConfig : Networks.WifiGoal.HotspotOn);
	}

	@Override
	public void disable(Context context) {
		if (Build.VERSION.SDK_INT >= 23) {
			if (!Settings.System.canWrite(context))
				return;
		}
		Networks.getInstance().setWifiGoal(Networks.WifiGoal.Off);
	}

	@Override
	public Intent getIntent(Context context) {
		PackageManager packageManager = context.getPackageManager();

		Intent i = new Intent();
		// Android 4(-ish)
		i.setClassName("com.android.settings", "com.android.settings.TetherSettings");
		ResolveInfo r = packageManager.resolveActivity(i, 0);
		if (r!=null){
			i.setClassName(r.activityInfo.packageName, r.activityInfo.name);
			return i;
		}
		// HTC roms
		i.setClassName("com.htc.WifiRouter", "com.htc.WifiRouter.WifiRouter");
		r = packageManager.resolveActivity(i, 0);
		if (r!=null){
			i.setClassName(r.activityInfo.packageName, r.activityInfo.name);
			return i;
		}
		// AOSP v2(-ish)
		i.setClassName("com.android.settings", "com.android.settings.wifi.WifiApSettings");
		r = packageManager.resolveActivity(i, 0);
		if (r!=null){
			i.setClassName(r.activityInfo.packageName, r.activityInfo.name);
			return i;
		}
		return null;
	}

	@Override
	public boolean isUsable(){
		return isOn() && isServalConfig();
	}
}
