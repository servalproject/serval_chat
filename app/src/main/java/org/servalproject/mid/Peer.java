package org.servalproject.mid;

import android.os.Handler;
import android.util.Log;

import org.servalproject.servaldna.RouteLink;
import org.servalproject.servaldna.ServalDCommand;
import org.servalproject.servaldna.SubscriberId;

/**
 * Created by jeremy on 4/05/16.
 */
public final class Peer {
	private static final String TAG ="Peer";

	Peer(Handler handler, SubscriberId sid){
		this.sid = sid;
		observers = new ObserverSet<>(handler, this);
	}

	public final ObserverSet<Peer> observers;
	public final SubscriberId sid;

	ServalDCommand.LookupResult lookup;
	public String getDid(){
		return lookup==null?null:lookup.did;
	}
	public String getName(){
		return lookup==null?null:lookup.name;
	}

	void update(ServalDCommand.LookupResult result){
		lookup = result;
		Log.v(TAG, "Updated details "+result.toString());
		observers.onUpdate();
	}

	RouteLink link;
	public boolean isReachable(){
		return link!=null;
	}

	void update(RouteLink route){
		link = route.isReachable() ? route : null;
		Log.v(TAG, "Updated route "+route.toString());
		observers.onUpdate();
	}
}
