package org.servalproject.mid;

import android.content.SharedPreferences;
import android.util.Log;

import org.servalproject.servalchat.BuildConfig;
import org.servalproject.servaldna.AbstractId;
import org.servalproject.servaldna.BundleId;
import org.servalproject.servaldna.ServalDCommand;
import org.servalproject.servaldna.ServalDFailureException;
import org.servalproject.servaldna.rhizome.RhizomeListBundle;

/**
 * Created by jeremy on 12/04/17.
 */

public class SelfUpdater {

	private final BundleId ourBundle;
	private final Serval serval;
	private boolean inserted = false;
	private static final String TAG = "SelfUpdater";

	private static SelfUpdater instance;
	public static void init(Serval serval){
		if (BuildConfig.ManifestId==null)
			return;
		if (instance != null)
			throw new IllegalStateException();
		instance = new SelfUpdater(serval);
	}

	private SelfUpdater(Serval servalinst){
		this.serval = servalinst;
		try {
			ourBundle = new BundleId(BuildConfig.ManifestId);
		} catch (AbstractId.InvalidHexException e) {
			throw new IllegalStateException(e);
		}

		// if we haven't tried to insert this apk into rhizome, try it once.
		String added = serval.settings.getString("apk_added", null);
		inserted = BuildConfig.BuildStamp.equals(added);

		serval.rhizome.observers.addBackground(new Observer<Rhizome>() {
			@Override
			public void updated(Rhizome obj) {
				if (!obj.isEnabled())
					return;
				if (inserted)
					return;

				serval.runOnThreadPool(new Runnable() {
					@Override
					public void run() {
						rhizomeInsert();
					}
				});
			}
		});

		serval.rhizome.observerSet.addBackground(new ListObserver<RhizomeListBundle>() {
			@Override
			public void added(RhizomeListBundle obj) {
				if (obj.manifest.id.equals(ourBundle)){
					// TODO test if we should upgrade
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

	private void rhizomeInsert(){
		try {
			ServalDCommand.ManifestResult r =
					ServalDCommand.rhizomeImportZipBundle(serval.apkFile);
		} catch (ServalDFailureException e) {
			Log.v(TAG, e.getMessage(), e);
		}

		SharedPreferences.Editor e = serval.settings.edit();
		e.putString("apk_added", BuildConfig.BuildStamp);
		e.apply();
		// only try once, even if we failed
		inserted = true;
	}
}
