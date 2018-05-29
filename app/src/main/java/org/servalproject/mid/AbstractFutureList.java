package org.servalproject.mid;

import org.servalproject.servaldna.AbstractJsonList;
import org.servalproject.servaldna.ServalDInterfaceException;

import java.io.IOException;

/**
 * Created by jeremy on 30/01/17.
 */

public abstract class AbstractFutureList<T, E extends Exception>
		extends AbstractGrowingList<T, E>{
	private final ListObserverSet<T> observeFuture;
	private AbstractJsonList<T, E> futureList;
	private boolean polling = false;
	protected T last;

	protected AbstractFutureList(Serval serval) {
		super(serval);
		this.observeFuture = new ListObserverSet<>(serval);
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
			closeFuture();
		}
	}

	protected abstract AbstractJsonList<T, E> openFuture() throws ServalDInterfaceException, E, IOException;

	@Override
	protected void addingPastItem(T item) {
		if (last == null) {
			last = item;
			start();
		}
		super.addingPastItem(item);
	}

	@SuppressWarnings("unchecked")
	protected void addingFutureItem(T item) {
		boolean isLatest = true;

		if (last != null && item instanceof Comparable<?>)
			isLatest = ((Comparable<T>)item).compareTo(last) <0;
		if (isLatest)
			last = item;

		observeFuture.onAdd(item);
	}

	private Runnable readFuture = new Runnable() {
		@Override
		public void run() {
			while (polling) {
				try {
					AbstractJsonList<T, E> list = futureList = openFuture();
					if (list != null) {
						T item;
						while (polling && (item = list.next()) != null) {
							addingFutureItem(item);
						}
						// on graceful close from the server, restart
						list.close();
					}
					if (futureList == list)
						futureList = null;
				} catch (IOException e) {
					// ignore if we caused this deliberately in another thread.
					if (polling)
						throw new IllegalStateException(e);
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
		}
	};

	private void closeFuture(){
		AbstractJsonList<T, E> list = futureList;
		if (list != null) {
			try {
				list.close();
			} catch (IOException e) {
			}
			futureList = null;
		}
	}

	@Override
	public void close() {
		super.close();
		polling = false;
		closeFuture();
	}
}
