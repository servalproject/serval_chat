package org.servalproject.mid;

import android.util.Log;

import org.servalproject.servaldna.ServalDCommand;
import org.servalproject.servaldna.ServalDFailureException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jeremy on 3/05/16.
 */

public class Config {

	private Map<String, String> values = null;
	private Map<String, String> pending = new HashMap<>();
	private static final String deleteFlag = "deleteme";

	private static final String TAG="Config";

	void set(String name, String value){
		pending.put(name, value);
	}

	String get(String name){
		String value=null;
		if (pending.containsKey(name)) {
			value = pending.get(name);
			// don't use equals here
			if (value == deleteFlag)
				value = null;
		}else if(values!=null)
			value = values.get(name);
		return value;
	}

	void delete(String name){
		pending.put(name, deleteFlag);
	}

	public void sync() throws ServalDFailureException {
		if (pending.isEmpty())
			return;

		List<String> changes = new ArrayList<>();
		for (String key:pending.keySet()) {
			String value = pending.get(key);
			// don't use .equals() here
			if (value == deleteFlag) {
				changes.add("del");
				changes.add(key);
			}else{
				changes.add("set");
				changes.add(key);
				changes.add(value);
			}
		}
		changes.add("sync");
		ServalDCommand.configActions(changes.toArray(new Object[changes.size()]));
		ServalDCommand.ConfigItems items = ServalDCommand.getConfig();
		values = items.values;
		pending.clear();
	}
}
