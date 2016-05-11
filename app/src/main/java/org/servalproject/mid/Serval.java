package org.servalproject.mid;

import android.content.Context;
import android.os.*;

import org.servalproject.servaldna.ChannelSelector;
import org.servalproject.servaldna.ServalDCommand;
import org.servalproject.servaldna.ServalDFailureException;
import org.servalproject.servaldna.ServerControl;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Created by jeremy on 3/05/16.
 */
public class Serval {

	private static final String TAG = "Serval";

	static final int START = 1;
	static final int CPU_LOCK = 2;
	static final int SERVER_UP = 3;

	private Serval(Context context) throws IOException {
		File appFolder = context.getFilesDir().getParentFile();
		instancePath = new File(appFolder, "instance");
		uiHandler = new UIHandler();
		backgroundHandler = BackgroundHandler.create(this);

		server = new Server(this, context);
		rhizome = new Rhizome(this, context);
		config = new Config();

		selector = new ChannelSelector();
		knownPeers = new KnownPeers(this);

		// Do the rest of our startup process on the eventhandler thread
		backgroundHandler.replaceMessage(START, 0);
	}

	void startup(){
		try {
			ServalDCommand.setInstancePath(instancePath.getPath());
			// if sdcard is available, enable rhizome
			rhizome.updateRhizomeConfig();

			// roll a new restful api password, partly so we only parse config once on the critical path for startup
			// partly for slightly better security
			String restfulPassword = new BigInteger(130, new SecureRandom()).toString(32);
			config.set("api.restful.users." + ServerControl.restfulUsername + ".password", restfulPassword);
			config.set("interfaces.0.match", "eth0,tiwlan0,wlan0,wl0.1,tiap0");
			config.set("interfaces.0.default_route", "on");
			config.set("mdp.enable_inet", "on");

			config.sync();

			// TODO if debuggable, set log path to SDcard?
		} catch (ServalDFailureException e) {
			throw new IllegalStateException(e);
		}

		Thread serverThread=new Thread(server, "Servald");
		serverThread.start();
	}

	final UIHandler uiHandler;
	final BackgroundHandler backgroundHandler;
	public final Server server;
	public final Rhizome rhizome;
	public final Config config;
	public final KnownPeers knownPeers;
	final ChannelSelector selector;;
	final File instancePath;

	private static Serval instance;
	public static void start(Context appContext){
		try {
			instance = new Serval(appContext);
		} catch (IOException e) {
			// Yep, we want to crash (this shouldn't happen, but would completely break everything anyway)
			throw new IllegalStateException(e);
		}
	}

	public static Serval getInstance(){
		return instance;
	}
}
