package org.servalproject.mid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.Log;

import org.servalproject.servaldna.ServalDFailureException;

import java.io.File;

/**
 * Created by jeremy on 3/05/16.
 */
public class Rhizome extends BroadcastReceiver{
	private static final String TAG = "Rhizome";

	private final Context context;
	private final Serval serval;

	File rhizomeFolder;

	Rhizome(Serval serval, Context context){
		this.serval = serval;
		this.context = context;

		rhizomeFolder = getRhizomePath();
	}

	void onStart(){
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_EJECT);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		context.registerReceiver(this, filter);
	}

	private File getRhizomePath(){
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File folder = context.getExternalFilesDir(null);
			if (folder != null) {
				return new File(folder, "rhizome");
			}
		}
		return null;
	}

	void updateRhizomeConfig(){
		if (rhizomeFolder == null) {
			serval.config.set("rhizome.enable", "0");
			serval.config.delete("rhizome.datastore_path");
		}else{
			serval.config.set("rhizome.enable", "1");
			serval.config.set("rhizome.datastore_path", rhizomeFolder.getPath());
		}
	}

	void setRhizomeConfig(){
		File rhizomeFolder = getRhizomePath();

		if (rhizomeFolder == null && this.rhizomeFolder==null)
			return;
		if (rhizomeFolder != null && this.rhizomeFolder!=null && rhizomeFolder.equals(rhizomeFolder))
			return;

		this.rhizomeFolder = rhizomeFolder;
		updateRhizomeConfig();
		try {
			serval.config.sync();
		} catch (ServalDFailureException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if(action.equals(Intent.ACTION_MEDIA_EJECT) ||
				action.equals(Intent.ACTION_MEDIA_UNMOUNTED) ||
				action.equals(Intent.ACTION_MEDIA_MOUNTED))
			// redetect sdcard path & availability
			setRhizomeConfig();
	}
}
