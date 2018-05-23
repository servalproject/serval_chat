package org.servalproject.mid.networking;

import org.servalproject.mid.ListObserver;

public abstract class AbstractListObserver<T> implements ListObserver<T> {
	@Override
	public void added(T obj) {
		
	}

	@Override
	public void removed(T obj) {

	}

	@Override
	public void updated(T obj) {

	}

	@Override
	public void reset() {

	}
}
