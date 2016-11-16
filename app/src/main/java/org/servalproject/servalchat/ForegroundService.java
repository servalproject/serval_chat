package org.servalproject.servalchat;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;

/**
 * Created by jeremy on 19/10/16.
 * Create a notification so that android wont just kill our process while a network is viable
 */
public class ForegroundService extends Service {

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getBooleanExtra("foreground", false)) {
			// TODO open network info / control view
			Intent navIntent = MainActivity.getIntentFor(this, null, Navigation.Networking, null);
			PendingIntent pending = PendingIntent.getActivity(this, 0, navIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			NotificationCompat.Builder builder =
					new NotificationCompat.Builder(this)
							.setSmallIcon(R.mipmap.serval_head)
							.setContentTitle(getString(R.string.foreground_title))
							.setContentText(getString(R.string.foreground_text))
							.setContentIntent(pending);
			this.startForeground(-1, builder.build());
			return START_STICKY;
		} else {
			stopForeground(true);
			stopSelf();
			return START_NOT_STICKY;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
