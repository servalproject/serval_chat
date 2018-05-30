package org.servalproject.mid;

import android.os.Handler;
import android.os.Message;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by jeremy on 4/05/16.
 */
public class ObserverSet<T> implements Handler.Callback, IObserverSet<T> {
	private final Set<Observer<T>> UIobservers = new HashSet<>();
	private final Set<Observer<T>> backgroundObservers = new HashSet<>();

	private final CallbackHandler UIhandler;
	private final CallbackHandler backgroundHandler;
	private final T obj;
	private static final String TAG = "ObserverSet";

	private static final int UICallbacks = 1;
	private static final int BackgroundCallbacks = 2;

	public ObserverSet(Serval serval, T obj) {
		this.UIhandler = serval.uiHandler;
		this.backgroundHandler = serval.backgroundHandler;
		this.obj = obj;
	}

	public T getObj(){
		return obj;
	}

	@Override
	public void addUI(Observer<T> observer) {
		UIobservers.add(observer);
	}

	@Override
	public void removeUI(Observer<T> observer) {
		UIobservers.remove(observer);
	}

	@Override
	public void addBackground(Observer<T> observer) {
		backgroundObservers.add(observer);
	}

	@Override
	public void removeBackground(Observer<T> observer) {
		backgroundObservers.remove(observer);
	}

	public void onUpdate() {
		if (!UIobservers.isEmpty())
			UIhandler.sendEmptyMessage(this, UICallbacks);
		if (!backgroundObservers.isEmpty())
			backgroundHandler.sendEmptyMessage(this, BackgroundCallbacks);
	}

	@Override
	public boolean handleMessage(Message message) {
		Set<Observer<T>> observers;
		switch (message.what){
			case UICallbacks:
				observers = UIobservers;
				break;
			case BackgroundCallbacks:
				observers = backgroundObservers;
				break;
			default:
				return false;
		}
		for (Observer<T> observer : observers)
			observer.updated(obj);
		return true;
	}
}
