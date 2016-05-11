package org.servalproject.mid;

import android.os.Handler;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by jeremy on 4/05/16.
 */
public final class ObserverSet<T> implements Runnable {
	private final Set<Observer<T>> observers = new HashSet<>();

	private final Handler handler;
	private final T obj;

	ObserverSet(Handler handler, T obj){
		this.handler = handler;
		this.obj = obj;
	}

	public void add(Observer<T> observer){
		observers.add(observer);
	}

	public void remove(Observer<T> observer){
		observers.remove(observer);
	}

	void onUpdate(){
		if (observers.isEmpty())
			return;
		handler.post(this);
	}

	@Override
	public void run() {
		if (observers.isEmpty())
			return;
		for(Observer<T> observer:observers){
			observer.updated(obj);
		}
	}
}
