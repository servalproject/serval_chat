package org.servalproject.servalchat;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.servalproject.mid.SelfUpdater;
import org.servalproject.mid.Serval;
import org.servalproject.mid.networking.Networks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by jeremy on 11/05/16.
 */
public class App extends Application {

	private static boolean testing = false;
	public static boolean isTesting(){
		return testing;
	}

	public Intent getErrorIntent(Throwable e){
		String name = getString(R.string.app_name);
		StringWriter stack = new StringWriter();
		e.printStackTrace(new PrintWriter(stack));

		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL, new String[]{"androidmarket@servalproject.org"});
		i.putExtra(Intent.EXTRA_SUBJECT, "Unhandled Exception in "+name+" "+BuildConfig.VERSION_NAME+", "+e.getMessage());
		i.putExtra(Intent.EXTRA_TEXT, "The following Exception occurred in "+name+" "+BuildConfig.VERSION_NAME+" and was not caught\n\n"+
			e.getMessage()+"\n"+stack);

		Serval serval = Serval.getInstance();
		File logFile = new File(serval.instancePath, "serval.log");
		try {
			i.putExtra(Intent.EXTRA_STREAM, CustomFileProvider.forInstanceFile(serval, logFile));
		} catch (IOException e1) {
			Log.e("App", e1.getMessage(), e1);
		}
		return i;
	}

	private Thread.UncaughtExceptionHandler defaultHandler;
	private Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread t, Throwable e) {

			NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			PendingIntent pending = PendingIntent.getActivity(App.this, 0, getErrorIntent(e), PendingIntent.FLAG_ONE_SHOT);

			NotificationCompat.Builder builder =
					new NotificationCompat.Builder(App.this, App.CHANNEL_ID)
							.setAutoCancel(true)
							.setSmallIcon(R.mipmap.serval_head)
							.setContentTitle(App.this.getString(R.string.background_error))
							.setContentText(App.this.getString(R.string.please_email_log))
							.setContentIntent(pending);

			nm.notify("Error", 0, builder.build());
			defaultHandler.uncaughtException(t, e);
		}
	};

	public static String CHANNEL_ID = null;

	@Override
	public void onCreate() {
		super.onCreate();

		defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

		// is this a launch test after uploading to the play store?
		String testLabSetting = Settings.System.getString(this.getContentResolver(), "firebase.test.lab");
		if ("true".equals(testLabSetting))
			testing = true;

		if (!testing
				&& BuildConfig.BUILD_TYPE.equals("release")
				&& !BuildConfig.ReleaseType.equals("debug"))
			Thread.setDefaultUncaughtExceptionHandler(this.handler);

		// always start our daemon thread
		Serval serval = Serval.start(this);
		Networks.init(serval);
		Notifications.init(serval, this);
		if (testing && "alpha".equals(BuildConfig.ReleaseType))
			SampleData.init(serval);
		SelfUpdater.init(serval, this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
			CHANNEL_ID = NotificationChannel.DEFAULT_CHANNEL_ID;
			NotificationManager nm = getSystemService(NotificationManager.class);
			nm.createNotificationChannel(
					new NotificationChannel(
						CHANNEL_ID,
						getString(R.string.app_name),
						NotificationManager.IMPORTANCE_DEFAULT));
		}
	}
}
