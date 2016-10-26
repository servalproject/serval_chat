package org.servalproject.servalchat;

import android.app.Application;

import org.servalproject.mid.Serval;

/**
 * Created by jeremy on 11/05/16.
 */
public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		// always start our daemon thread
		Serval serval = Serval.start(this);
		Notifications.onStart(serval, this);
	}
}
