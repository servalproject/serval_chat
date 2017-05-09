package org.servalproject.servalchat;

import android.app.Application;
import android.provider.Settings;

import org.servalproject.mid.SelfUpdater;
import org.servalproject.mid.Serval;
import org.servalproject.mid.networking.Networks;

/**
 * Created by jeremy on 11/05/16.
 */
public class App extends Application {

	private static boolean testing = false;
	public static boolean isTesting(){
		return testing;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// is this a launch test after uploading to the play store?
		String testLabSetting = Settings.System.getString(this.getContentResolver(), "firebase.test.lab");
		if ("true".equals(testLabSetting))
			testing = true;

		// always start our daemon thread
		Serval serval = Serval.start(this);
		Networks.init(serval);
		Notifications.init(serval, this);
		if (testing && "alpha".equals(BuildConfig.ReleaseType))
			SampleData.init(serval);
		SelfUpdater.init(serval);
	}
}
