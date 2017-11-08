package org.servalproject.mid;

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
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.rhizome.RhizomeCommon;
import org.servalproject.servaldna.rhizome.RhizomeException;
import org.servalproject.servaldna.rhizome.RhizomeImportStatus;
import org.servalproject.servaldna.rhizome.RhizomeListBundle;
import org.servalproject.servaldna.rhizome.RhizomeManifest;
import org.servalproject.servaldna.rhizome.RhizomeManifestBundle;
import org.servalproject.servaldna.rhizome.RhizomeManifestParseException;
import org.servalproject.servaldna.rhizome.RhizomeManifestSizeException;
import org.servalproject.servaldna.rhizome.RhizomePayloadRawBundle;

import java.io.File;
import java.io.IOException;

/**
 * Created by jeremy on 12/04/17.
 */

public class SelfUpdater {

	private final BundleId ourBundle;
	private final Serval serval;
	private final Context context;
	private String addedBuild;
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
		addedBuild = serval.settings.getString("added_build", null);

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
			public void added(final RhizomeListBundle obj) {
				// note, our insert into rhizome should trigger this callback too
				if (obj.manifest.id.equals(ourBundle) && obj.manifest.version > BuildConfig.ManifestVersion){
					serval.runOnThreadPool(new Runnable() {
						@Override
						public void run() {
							exportApk(obj.manifest);
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

	private File getUpgradeFile(){
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
			return null;

		File folder = context.getExternalFilesDir(null);
		if (folder == null)
			return null;

		return new File(folder, BuildConfig.ReleaseType + "_upgrade.apk");
	}

	private void uptoDate(){
		File upgradeFile = getUpgradeFile();
		if (upgradeFile!=null)
			upgradeFile.delete();
		// stop checking rhizome until re-install or new bundle arrives
		SharedPreferences.Editor e = serval.settings.edit();
		e.putString("added_build", addedBuild = BuildConfig.BuildStamp);
		e.apply();
	}

	private RhizomeManifest ourManifest(){
		try {
			RhizomeManifestBundle result = serval.getResultClient().rhizomeManifest(ourBundle);
			if (result!=null)
				return result.manifest;
		} catch (ServalDInterfaceException |
				IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	private void exportApk() {
		RhizomeManifest manifest = ourManifest();
		if (manifest!=null)
			exportApk(manifest);
	}

	private void exportApk(RhizomeManifest manifest){
		try {
			File upgradeFile = getUpgradeFile();
			if (upgradeFile == null)
				return;

			if (upgradeFile.length() != manifest.filesize + manifest.toTextFormat().length - 2) {
				// If the file is missing, or the wrong size, export it again
				// TODO binary compare manifest text in case we release two builds of the same length?
				// or just remember the version we exported?

				RhizomePayloadRawBundle payload = serval.getResultClient().rhizomePayloadRaw(ourBundle);
				if (payload == null || payload.rawPayloadInputStream == null)
					return;

				RhizomeCommon.WriteBundleZip(payload, upgradeFile);
			}

			// we can stop checking rhizome while this file exists
			SharedPreferences.Editor e = serval.settings.edit();
			e.putString("added_build", addedBuild = BuildConfig.BuildStamp);
			e.apply();

			notifyUpgrade(upgradeFile);
		} catch (RhizomeManifestSizeException |
				ServalDInterfaceException |
				IOException e) {
			Log.v(TAG, e.getMessage(),e);
		}
	}

	private void notifyUpgrade(){
		File upgradeFile = getUpgradeFile();
		if (upgradeFile == null || !upgradeFile.exists())
			return;
		notifyUpgrade(upgradeFile);
	}

	private void notifyUpgrade(File upgradeFile){
		Intent i = new Intent("android.intent.action.VIEW")
				.setType("application/vnd.android.package-archive")
				.setClassName("com.android.packageinstaller",
						"com.android.packageinstaller.PackageInstallerActivity")
				.setData(Uri.fromFile(upgradeFile))
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
		if (BuildConfig.BuildStamp.equals(addedBuild)) {
			// check if we have seen a new version
			notifyUpgrade();
			return;
		}

		try {
			RhizomeManifest manifest = ourManifest();

			if (manifest != null) {
				if (manifest.version == BuildConfig.ManifestVersion) {
					uptoDate();
					return;
				} else if (manifest.version > BuildConfig.ManifestVersion) {
					exportApk(manifest);
					return;
				}
			}

			RhizomeImportStatus status = serval.getResultClient().rhizomeImportZip(serval.apkFile);
			switch (status.bundleStatus) {
				// possible race conditions with rhizome syncing an apk over the network
				case OLD:
					exportApk();
					break;
				case SAME:
				case NEW:
					uptoDate();
					break;

				case INVALID:
					// no manifest, or other permanent error

				case BUSY:
				case NO_ROOM:
					// allow another later attempt
					return;

				default:
					throw new IllegalStateException("Import returned " + status.bundleStatus);
			}
		} catch (ServalDInterfaceException
				| RhizomeException
				| RhizomeManifestSizeException
				| RhizomeManifestParseException
				| IOException e) {
			Log.v(TAG, e.getMessage(),e);
		}
	}
}
