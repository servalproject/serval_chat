package org.servalproject.mid;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class ObserverProxy<S,I> implements IObserverSet<I>, Observer<S> {
	private final IObserverSet<S> source;
	private final I item;
	private final Set<Observer<I>> UIobservers = new HashSet<>();
	private final Set<Observer<I>> backgroundObservers = new HashSet<>();
	private final CallbackHandler uiHandler;

	public ObserverProxy(CallbackHandler uiHandler, IObserverSet<S> source, I item){
		this.uiHandler = uiHandler;
		this.source = source;
		this.item = item;
	}

	@Override
	public void addUI(Observer<I> observer) {
		if (UIobservers.isEmpty())
			source.addUI(this);
		UIobservers.add(observer);
	}

	@Override
	public void removeUI(Observer<I> observer) {
		UIobservers.remove(observer);
		if (UIobservers.isEmpty())
			source.removeUI(this);
	}

	@Override
	public void addBackground(Observer<I> observer) {
		if (backgroundObservers.isEmpty())
			source.addBackground(this);
		backgroundObservers.add(observer);
	}

	@Override
	public void removeBackground(Observer<I> observer) {
		backgroundObservers.remove(observer);
		if (backgroundObservers.isEmpty())
			source.removeBackground(this);
	}

	@Override
	public I getObj() {
		return item;
	}

	@Override
	public void updated(S obj) {
		Set<Observer<I>> observers = (uiHandler.isOnThread())?UIobservers:backgroundObservers;
		for(Observer<I> o : observers)
			o.updated(item);
	}
}
