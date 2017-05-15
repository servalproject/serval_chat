package org.servalproject.mid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.servalproject.servalchat.BuildConfig;
import org.servalproject.servalchat.R;
import org.servalproject.servaldna.AbstractId;
import org.servalproject.servaldna.BundleId;
import org.servalproject.servaldna.ServalDCommand;
import org.servalproject.servaldna.ServalDFailureException;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.rhizome.RhizomeBundleStatus;
import org.servalproject.servaldna.rhizome.RhizomeListBundle;

import java.io.File;

/**
 * Created by jeremy on 12/04/17.
 */

public class SelfUpdater {

	private final BundleId ourBundle;
	private final Serval serval;
	private final Context context;
	private String addedBuild;
	private long rhizomeVersion=-1;
	private static final String TAG = "SelfUpdater";

	private static SelfUpdater instance;
	public static void init(Serval serval, Context context){
		// If this build doesn't know a manifest id, then we can't update it
		if (BuildConfig.ManifestId==null)
			return;
		if (instance != null)
			throw new IllegalStateException();
		instance = new SelfUpdater(serval, context);
	}

	private SelfUpdater(Serval servalinst, Context context){
		this.context = context;
		this.serval = servalinst;
		try {
			ourBundle = new BundleId(BuildConfig.ManifestId);
		} catch (AbstractId.InvalidHexException e) {
			throw new IllegalStateException(e);
		}

		addedBuild = serval.settings.getString("apk_added", null);
		rhizomeVersion = serval.settings.getLong("rhizome_version", -1);

		serval.rhizome.observers.addBackground(new Observer<Rhizome>() {
			@Override
			public void updated(Rhizome obj) {
				if (!obj.isEnabled())
					return;
				serval.runOnThreadPool(new Runnable() {
					@Override
					public void run() {
						rhizomeStarted();
					}
				});
			}
		});

		serval.rhizome.observerSet.addBackground(new ListObserver<RhizomeListBundle>() {
			@Override
			public void added(RhizomeListBundle obj) {
				if (obj.manifest.id.equals(ourBundle) && obj.manifest.version != rhizomeVersion){
					// note, our insert into rhizome should trigger this
					SharedPreferences.Editor e = serval.settings.edit();
					e.putLong("rhizome_version", rhizomeVersion = obj.manifest.version);
					e.apply();

					serval.runOnThreadPool(new Runnable() {
						@Override
						public void run() {
							notifyUpgrade();
						}
					});
				}
			}

			@Override
			public void removed(RhizomeListBundle obj) {

			}

			@Override
			public void updated(RhizomeListBundle obj) {

			}

			@Override
			public void reset() {

			}
		});
	}

	private void notifyUpgrade(){
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
			return;

		File folder = context.getExternalFilesDir(null);
		if (folder == null)
			return;

		File newVersion = new File(folder, BuildConfig.ReleaseType + "_upgrade.apk");

		if (!BuildConfig.BuildStamp.equals(addedBuild)
				|| rhizomeVersion <= BuildConfig.ManifestVersion) {
			newVersion.delete();
			return;
		}

		// create a combined payload and manifest
		if (!newVersion.exists()) {
			try {
				ServalDCommand.ManifestResult r =
						ServalDCommand.rhizomeExportZipBundle(ourBundle, newVersion);
				// TODO, double check the version exported from rhizome
			} catch (ServalDInterfaceException e) {
				// anything could go wrong, might be out of disk space
				Log.v(TAG, e.getMessage(), e);
				return;
			}
		}

		Intent i = new Intent("android.intent.action.VIEW")
				.setType("application/vnd.android.package-archive")
				.setClassName("com.android.packageinstaller",
						"com.android.packageinstaller.PackageInstallerActivity")
				.setData(Uri.fromFile(newVersion))
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i,
				PendingIntent.FLAG_ONE_SHOT);

		NotificationCompat.Builder builder =
				new NotificationCompat.Builder(context)
						.setAutoCancel(true)
						.setSmallIcon(R.mipmap.serval_head)
						.setContentTitle(context.getString(R.string.new_version_title))
						.setContentText(context.getString(R.string.new_version_message))
						.setContentIntent(pendingIntent);

		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify("Upgrade", 1, builder.build());
	}

	private void rhizomeStarted(){
		try {
			if (!BuildConfig.BuildStamp.equals(addedBuild)) {
				ServalDCommand.ManifestResult r =
						ServalDCommand.rhizomeImportZipBundle(serval.apkFile);
				SharedPreferences.Editor e = serval.settings.edit();
				switch (r.getBundleStatus()) {
					case OLD:
					case NEW:
					case SAME:
						e.putLong("rhizome_version", rhizomeVersion = r.version);
						e.putString("apk_added", addedBuild = BuildConfig.BuildStamp);
						break;

					case BUSY:
					case NO_ROOM:
						// allow another later attempt
						break;

					case INVALID:
						// no manifest, or other permanent error
						e.putLong("rhizome_version", rhizomeVersion = -1);
						e.putString("apk_added", addedBuild = BuildConfig.BuildStamp);
						break;

					default:
						throw new IllegalStateException("Import returned " + r.getBundleStatus());
				}
				e.apply();
			}
			notifyUpgrade();
		} catch (ServalDInterfaceException e) {
			throw new IllegalStateException(e);
		}
	}
}
