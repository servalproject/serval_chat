package org.servalproject.mid;

import android.os.Handler;
import android.util.Log;

import org.servalproject.servaldna.RouteLink;
import org.servalproject.servaldna.ServalDCommand;
import org.servalproject.servaldna.Subscriber;

/**
 * Created by jeremy on 4/05/16.
 */
public final class Peer implements Comparable<Peer>{
	private static final String TAG ="Peer";

	private static long nextId=0;
	private final long id;

	Peer(Handler handler, Subscriber subscriber){
		this.subscriber = subscriber;
		observers = new ObserverSet<>(handler, this);
		id = nextId++;
	}

	public final ObserverSet<Peer> observers;
	private Subscriber subscriber;
	public Subscriber getSubscriber(){
		return subscriber;
	}
	public void updateSubscriber(Subscriber subscriber){
		if (this.subscriber.sid.equals(subscriber.sid)
				&& this.subscriber.signingKey == null) {
			this.subscriber = subscriber;
		}
	}

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

	public boolean isContact(){
		return false;
	}

	public boolean isBlocked(){
		return false;
	}

	void update(RouteLink route){
		link = route.isReachable() ? route : null;
		Log.v(TAG, "Updated route "+route.toString());
		observers.onUpdate();
	}

	public long getId() {
		// return a stable id, for UI list binding.
		return id;
	}

	@Override
	public String toString() {
		return "Peer{" +
				"subscriber=" + subscriber +
				", lookup=" + lookup +
				", link=" + link +
				'}';
	}

	private String feedName;

	public String displayName(){
		String n = feedName;
		if (n == null || "".equals(n))
			n = getName();
		if (n == null || "".equals(n))
			n = getDid();
		if (n == null || "".equals(n))
			n = subscriber.sid.abbreviation();
		return n;
	}

	public void updateFeedName(String name){
		if (feedName == null && name == null)
			return;
		if (name!=null && name.equals(feedName))
			return;
		feedName = name;
		observers.onUpdate();
	}

	public MessageFeed getFeed(){
		if (subscriber.signingKey==null)
			return null;
		return new MessageFeed(Serval.getInstance(), this);
	}

	@Override
	public int compareTo(Peer another) {
		return this.displayName().compareTo(another.displayName());
	}
}
