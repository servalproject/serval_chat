package org.servalproject.mid;

import org.servalproject.servaldna.AbstractJsonList;
import org.servalproject.servaldna.ServalDInterfaceException;

import java.io.IOException;

/**
 * Created by jeremy on 11/10/16.
 */
public abstract class AbstractGrowingList<T, E extends Exception> implements IObservableList<T, E> {
	protected final Serval serval;
	private final ListObserverSet<T> observeFuture;
	private boolean hasMore = true;
	private boolean polling = false;
	private boolean closed = false;
	private AbstractJsonList<T, E> pastList;
	private AbstractJsonList<T, E> futureList;

	protected AbstractGrowingList(Serval serval) {
		this.serval = serval;
		this.observeFuture = new ListObserverSet<>(serval.uiHandler);
	}

	protected void start() {
		if (polling || !observeFuture.hasObservers())
			return;
		polling = true;
		serval.runOnThreadPool(readFuture);
	}

	public void observe(ListObserver<T> observer) {
		observeFuture.add(observer);
		start();
	}

	@Override
	public void stopObserving(ListObserver<T> observer) {
		observeFuture.remove(observer);
		if (!observeFuture.hasObservers()) {
			polling = false;
			if (futureList != null) {
				try {
					futureList.close();
				} catch (IOException e) {
				}
				futureList = null;
			}
		}
	}

	protected abstract AbstractJsonList<T, E> openPast() throws ServalDInterfaceException, E, IOException;

	protected abstract AbstractJsonList<T, E> openFuture() throws ServalDInterfaceException, E, IOException;

	protected void addingFutureItem(T item) {
		observeFuture.onAdd(item);
	}

	protected void addingPastItem(T item) {
	}

	private Runnable readFuture = new Runnable() {
		@Override
		public void run() {
			try {
				while (polling) {
					AbstractJsonList<T, E> list = futureList = openFuture();
					T item;
					while (polling && (item = list.next()) != null) {
						addingFutureItem(item);
					}
					// on graceful close, restart
					list.close();
					futureList = null;
				}
			} catch (IOException e) {
				// ignore if we caused this deliberately in another thread.
				if (polling)
					throw new IllegalStateException(e);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	};


	@Override
	public T next() throws ServalDInterfaceException, E, IOException {
		if (!hasMore)
			return null;
		if (pastList == null)
			pastList = openPast();

		T item = pastList.next();
		addingPastItem(item);

		if (item == null) {
			hasMore = false;
			pastList.close();
			pastList = null;
			return null;
		}
		return item;
	}

	@Override
	public void close() {
		hasMore = false;
		polling = false;
		closed = true;
		if (pastList != null) {
			try {
				pastList.close();
			} catch (IOException e) {
			}
			pastList = null;
		}
		if (futureList != null) {
			try {
				futureList.close();
			} catch (IOException e) {
			}
			futureList = null;
		}
	}
}
