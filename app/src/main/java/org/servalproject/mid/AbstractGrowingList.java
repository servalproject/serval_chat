package org.servalproject.mid;

import org.servalproject.servaldna.AbstractJsonList;
import org.servalproject.servaldna.ServalDInterfaceException;

import java.io.IOException;

/**
 * Created by jeremy on 11/10/16.
 */
public abstract class AbstractGrowingList<T, E extends Exception>
		implements IObservableList<T, E>{
	protected final Serval serval;
	protected boolean hasMore = true;
	private boolean closed = false;
	private AbstractJsonList<T, E> pastList;

	protected AbstractGrowingList(Serval serval) {
		this.serval = serval;
	}

	protected abstract AbstractJsonList<T, E> openPast() throws ServalDInterfaceException, E, IOException;

	protected void addingPastItem(T item) {
	}

	@Override
	public T next() throws ServalDInterfaceException, E, IOException {
		if (!hasMore)
			return null;
		if (pastList == null)
			pastList = openPast();

		T item = pastList.next();
		if (item == null) {
			hasMore = false;
			pastList.close();
			pastList = null;
		}
		addingPastItem(item);
		return item;
	}

	@Override
	public void close() {
		hasMore = false;
		closed = true;
		if (pastList != null) {
			try {
				pastList.close();
			} catch (IOException e) {
			}
			pastList = null;
		}
	}

	@Override
	public void observe(ListObserver<T> observer) {
		// NOOP
	}

	@Override
	public void stopObserving(ListObserver<T> observer) {
		// NOOP
	}
}
