package org.servalproject.mid;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.HandlerThread;
import android.preference.PreferenceManager;

import org.servalproject.servalchat.App;
import org.servalproject.servalchat.BuildConfig;
import org.servalproject.servaldna.ChannelSelector;
import org.servalproject.servaldna.ServalDClient;
import org.servalproject.servaldna.ServalDCommand;
import org.servalproject.servaldna.ServalDFailureException;
import org.servalproject.servaldna.ServalDInterfaceException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by jeremy on 3/05/16.
 */
public class Serval {

	private static final String TAG = "Serval";

	private Serval(Context context) throws IOException {
		this.context = context;
		this.apkFile = new File(context.getPackageCodePath());
		settings = PreferenceManager.getDefaultSharedPreferences(context);
		File appFolder = context.getFilesDir().getParentFile();
		instancePath = new File(appFolder, "instance");
		uiHandler = new CallbackHandler(context.getMainLooper());

		// default interface types, collected from various old phones
		interfaces.put(WIFI_INTERFACE, "eth0,wlan0,tiwlan0,wlp1s0");
		interfaces.put(WIFI_DIRECT_INTERFACE, "p2p0");
		interfaces.put(HOTSPOT_INTERFACE, "wl0.1,tiap0,ap0");

		// get the actual wifi interfaces from system properties, if they are defined
		for(String prop : new String[]{WIFI_INTERFACE, WIFI_DIRECT_INTERFACE, HOTSPOT_INTERFACE}){
			String value = System.getProperty(prop);
			if (value != null)
				interfaces.put(prop, value);
		}

		HandlerThread handlerThread = new HandlerThread("BackgroundHandler");
		handlerThread.start();
		backgroundHandler = new CallbackHandler(handlerThread.getLooper());

		backgroundQueue = new SynchronousQueue<>();
		backgroundThreads = new ThreadPoolExecutor(3, Integer.MAX_VALUE, 5, TimeUnit.SECONDS, backgroundQueue);

		server = new Server(this, context);
		rhizome = new Rhizome(this, context);
		config = new Config();

		selector = new ChannelSelector();
		knownPeers = new KnownPeers(this);
		identities = new Identities(this);

		// Do the rest of our startup process on a background thread
		backgroundHandler.post(new Runnable() {
			@Override
			public void run() {
				startup();
			}
		});
	}

	public static final String WIFI_INTERFACE="wifi.interface";
	public static final String WIFI_DIRECT_INTERFACE="wifi.direct.interface";
	public static final String HOTSPOT_INTERFACE="wifi.tethering.interface";

	private Map<String, String> interfaces = new HashMap<>();
	public void setInterface(String type, String name) throws ServalDFailureException {
		if (name.equals(interfaces.get(type)))
			return;

		interfaces.put(type, name);
		config.set("interfaces.2.match", getInterfaces());
		config.sync();
	}

	private String getInterfaces(){
		StringBuilder sb = new StringBuilder();
		for(String i : interfaces.values()){
			if ("".equals(i)||i==null)
				continue;
			if (sb.length()>0)
				sb.append(",");
			sb.append(i);
		}
		return sb.toString();
	}

	private void startup() {
		try {
			ServalDCommand.setInstancePath(instancePath.getPath());
			// if sdcard is available, enable rhizome
			rhizome.updateRhizomeConfig();

			// roll a new restful api password, partly so we only parse config once on the critical path for startup
			// partly for slightly better security
			restfulPassword = new BigInteger(130, new SecureRandom()).toString(32);
			config.set("api.restful.users." + restfulUsername + ".password", restfulPassword);
			config.set("api.restful.newsince_timeout", String.valueOf(60*60*24)); // 1 day...
			// Match any wifi or cabled ethernet network interface
			config.set("interfaces.0.match", "*");
			config.set("interfaces.0.match_type", "wifi");
			config.set("interfaces.0.type", "wifi");
			config.set("interfaces.0.default_route", "on");
			config.set("interfaces.1.match", "*");
			config.set("interfaces.1.match_type", "ethernet");
			config.set("interfaces.1.type", "ethernet");
			config.set("interfaces.1.default_route", "on");

			// fall back to these interface names if we can't read from /sys/net/<name>/
			config.set("interfaces.2.match", getInterfaces());
			config.set("interfaces.2.match_type", "other");
			config.set("interfaces.2.type", "wifi");
			config.set("interfaces.2.default_route", "on");
			config.set("mdp.enable_inet", "on");
			config.set("log.android.show_pid", "0");
			config.set("log.android.show_time", "0");

			if (App.isTesting() || BuildConfig.BUILD_TYPE.equals("debug")){
				config.set("log.android.level", "DEBUG");
			} else {
				config.set("log.android.level", "WARN");
			}

			config.sync();

			// TODO if debuggable, set log path to SDcard?
		} catch (ServalDFailureException e) {
			throw new IllegalStateException(e);
		}

		Thread serverThread = new Thread(server, "Servald");
		serverThread.start();
	}

	public final CallbackHandler uiHandler;
	public final CallbackHandler backgroundHandler;
	public final Context context;
	public final File apkFile;
	public final Server server;
	public final Rhizome rhizome;
	public final Config config;
	public final KnownPeers knownPeers;
	public final Identities identities;
	public final SharedPreferences settings;
	private final BlockingQueue<Runnable> backgroundQueue;
	private final ThreadPoolExecutor backgroundThreads;
	private String restfulUsername = "ServalDClient";
	private String restfulPassword;
	private ServalDClient client;
	public final ChannelSelector selector;
	public final File instancePath;

	void onServerStarted() {
		try {
			client = new ServalDClient(server.getHttpPort(), restfulUsername, restfulPassword);
		} catch (ServalDInterfaceException e) {
			throw new IllegalStateException(e);
		}
		identities.onStart();
		knownPeers.onStart();
		rhizome.onStart();
		// TODO trigger other startup here
		server.onStart();
	}

	ServalDClient getResultClient() {
		if (client == null)
			throw new IllegalStateException();
		return client;
	}

	public void runOnThreadPool(Runnable r) {
		backgroundThreads.execute(r);
	}

	public void runOnBackground(Runnable r) {
		backgroundHandler.post(r);
	}

	public void runReplaceDelayed(Runnable r, int delay) {
		backgroundHandler.removeCallbacks(r);
		backgroundHandler.postDelayed(r, delay);
	}
	public void runDelayed(Runnable r, int delay) {
		backgroundHandler.postDelayed(r, delay);
	}

	private static Serval instance;

	public static Serval start(Context appContext) {
		if (instance != null)
			throw new IllegalStateException("instance already created!");
		try {
			instance = new Serval(appContext.getApplicationContext());
			return instance;
		} catch (IOException e) {
			// Yep, we want to crash (this shouldn't happen, but would completely break everything anyway)
			throw new IllegalStateException(e);
		}
	}

	public static Serval getInstance() {
		if (instance == null)
			throw new IllegalStateException();
		return instance;
	}

}
