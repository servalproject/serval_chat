package org.servalproject.mid;

import android.os.Handler;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by jeremy on 4/05/16.
 */
public class ObserverSet<T> implements Runnable {
	private final Set<Observer<T>> observers = new HashSet<>();

	private final Handler handler;
	private final T obj;
	private static final String TAG="ObserverSet";

	ObserverSet(Handler handler, T obj){
		this.handler = handler;
		this.obj = obj;
	}

	public void add(Observer<T> observer){
		Log.v(TAG, "Adding observer");
		observers.add(observer);
	}

	public void remove(Observer<T> observer){
		Log.v(TAG, "Removing observer");
		observers.remove(observer);
	}

	void onUpdate(){
		Log.v(TAG, "Updated: "+obj.toString());
		if (observers.isEmpty())
			return;
		handler.post(this);
	}

	@Override
	public void run() {
		if (observers.isEmpty())
			return;
		Log.v(TAG, "Observing change: "+obj.toString());
		for(Observer<T> observer:observers){
			observer.updated(obj);
		}
	}
}
