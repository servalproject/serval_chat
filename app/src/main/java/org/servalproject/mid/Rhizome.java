package org.servalproject.mid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.Log;

import org.servalproject.json.JsonParser;
import org.servalproject.servaldna.ServalDFailureException;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.ServalDUnexpectedHttpStatus;
import org.servalproject.servaldna.rhizome.RhizomeBundleList;
import org.servalproject.servaldna.rhizome.RhizomeListBundle;

import java.io.File;
import java.io.IOException;

/**
 * Created by jeremy on 3/05/16.
 */
public class Rhizome extends BroadcastReceiver {
	private static final String TAG = "Rhizome";

	private final Context context;
	private final Serval serval;
	public final ListObserverSet<RhizomeListBundle> observerSet;
	public final ObserverSet<Rhizome> observers;

	private File rhizomeFolder;
	private boolean watching = false;

	Rhizome(Serval serval, Context context) {
		this.serval = serval;
		this.context = context;
		rhizomeFolder = getRhizomePath();
		observerSet = new ListObserverSet<>(serval);
		observers = new ObserverSet<>(serval, this);
	}

	// Since anything could go wrong when the daemon attempts to open the database,
	// setting the rhizome content folder is not sufficient to ensure future rhizome
	// requests will succeed. So, only consider rhizome to be enabled if we have
	// successfully opened our rhizomeListBundlesSince request
	public boolean isEnabled(){
		return rhizomeFolder!=null && watching;
	}

	void onStart() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_EJECT);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		context.registerReceiver(this, filter);

		if (rhizomeFolder!=null)
			serval.runOnThreadPool(watchBundles);

		observers.onUpdate();
	}

	private void setWatching(boolean value){
		if (watching == value)
			return;
		watching=value;
		observers.onUpdate();
	}

	private String token = null;
	private RhizomeBundleList watchList = null;
	private Runnable watchBundles = new Runnable() {
		@Override
		public void run() {
			try {
				while (rhizomeFolder != null) {
					// TODO add a magic token for the current end of list
					if (token == null) {
						RhizomeBundleList lastList = serval.getResultClient().rhizomeListBundles();
						try {
							RhizomeListBundle lastBundle = lastList.next();
							token = (lastBundle == null) ? "" : lastBundle.token;
						} finally {
							lastList.close();
						}
					}
					RhizomeBundleList list = watchList = serval.getResultClient().rhizomeListBundlesSince(token);
					setWatching(true);
					try {
						RhizomeListBundle bundle;
						while ((bundle = list.next()) != null) {
							token = bundle.token;
							observerSet.onAdd(bundle);
						}
					} catch (IOException e) {
						if (list == watchList)
							throw new IllegalStateException(e);
					} finally {
						list.close();
						if (list == watchList)
							watchList = null;
					}
					Thread.sleep(500);
				}
			} catch (ServalDUnexpectedHttpStatus e){
				if (e.responseCode!=404)
					throw new IllegalStateException(e);
			} catch (InterruptedException |
					ServalDInterfaceException |
					JsonParser.JsonParseException |
					IOException e) {
				throw new IllegalStateException(e);
			} finally {
				setWatching(false);
			}
		}
	};

	private File getRhizomePath() {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File folder = context.getExternalFilesDir(null);
			if (folder != null) {
				return new File(folder, "rhizome");
			}
		}
		return null;
	}

	void updateRhizomeConfig() {
		if (rhizomeFolder == null) {
			serval.config.set("rhizome.enable", "0");
			serval.config.delete("rhizome.datastore_path");
		} else {
			serval.config.set("rhizome.enable", "1");
			serval.config.set("rhizome.datastore_path", rhizomeFolder.getPath());
		}
	}

	void setRhizomeConfig() {
		File rhizomeFolder = getRhizomePath();

		if (rhizomeFolder == null && this.rhizomeFolder == null)
			return;
		if (rhizomeFolder != null && this.rhizomeFolder != null && this.rhizomeFolder.equals(rhizomeFolder))
			return;

		if (watchList != null && rhizomeFolder == null) {
			RhizomeBundleList list = watchList;
			watchList = null;
			try {
				list.close();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		this.rhizomeFolder = rhizomeFolder;
		updateRhizomeConfig();
		try {
			serval.config.sync();
		} catch (ServalDFailureException e) {
			throw new IllegalStateException(e);
		}
		if (rhizomeFolder!=null)
			serval.runOnThreadPool(watchBundles);
		observers.onUpdate();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_MEDIA_EJECT) ||
				action.equals(Intent.ACTION_MEDIA_UNMOUNTED) ||
				action.equals(Intent.ACTION_MEDIA_MOUNTED))
			// redetect sdcard path & availability
			setRhizomeConfig();
	}
}
