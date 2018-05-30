package org.servalproject.servalchat.views;

import android.util.Log;

import org.servalproject.mid.Observer;
import org.servalproject.mid.IObserverSet;
import org.servalproject.servalchat.navigation.ILifecycle;

public abstract class UIObserver<T> implements ILifecycle, Observer<T> {
	private final IObserverSet<T> source;

	protected UIObserver(IObserverSet<T> source){
		this.source = source;
	}

	@Override
	public abstract void updated(T obj);

	@Override
	public void onDetach(boolean configChange) {

	}

	@Override
	public void onVisible() {
		source.addUI(this);
		updated(source.getObj());
	}

	@Override
	public void onHidden() {
		source.removeUI(this);
	}
}
