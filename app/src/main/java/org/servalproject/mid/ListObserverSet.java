package org.servalproject.mid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by jeremy on 11/05/16.
 */
public class ListObserverSet<T> {
	private final Set<ListObserver<T>> observers = new HashSet<>();
	private final Set<ListObserver<T>> backgroundObservers = new HashSet<>();

	private static final int ADD = 1;
	private static final int REMOVE = 2;
	private static final int UPDATE = 3;
	private static final int RESET = 4;
	private int generation = 0;

	private final CallbackHandler uiHandler;
	private final CallbackHandler backgroundHandler;

	public ListObserverSet(Serval serval) {
		this.uiHandler = serval.uiHandler;
		this.backgroundHandler = serval.backgroundHandler;
	}

	public int add(ListObserver<T> observer) {
		observers.add(observer);
		return generation;
	}

	public int remove(ListObserver<T> observer) {
		observers.remove(observer);
		return generation;
	}

	public int addBackground(ListObserver<T> observer) {
		backgroundObservers.add(observer);
		return generation;
	}

	public int removeBackground(ListObserver<T> observer) {
		backgroundObservers.remove(observer);
		return generation;
	}

	public boolean hasObservers() {
		return !observers.isEmpty();
	}

	private void onChange(T t, int what){
		generation++;
		if (!observers.isEmpty())
			uiHandler.sendMessage(uiMessageHandler, t, what);
		if (!backgroundObservers.isEmpty())
			backgroundHandler.sendMessage(backgroundMessageHandler, t, what);
	}

	public void onAdd(T t) {
		onChange(t, ADD);
	}

	public void onRemove(T t) {
		onChange(t, REMOVE);
	}

	public void onUpdate(T t) {
		onChange(t, UPDATE);
	}

	public void onReset() {
		onChange(null, RESET);
	}

	private CallbackHandler.MessageHandler<T> uiMessageHandler = new CallbackHandler.MessageHandler<T>() {
		@Override
		public void handleMessage(T obj, int what) {
			handle(observers, obj, what);
		}
	};

	private CallbackHandler.MessageHandler<T> backgroundMessageHandler = new CallbackHandler.MessageHandler<T>() {
		@Override
		public void handleMessage(T obj, int what) {
			handle(backgroundObservers, obj, what);
		}
	};

	private void handle(Set<ListObserver<T>> observers, T obj, int what) {
		if (observers.isEmpty())
			return;
		// clone the list so handlers can remove while we are iterating
		List<ListObserver<T>> notify = new ArrayList<>(observers);
		for (ListObserver<T> observer : notify) {
			switch (what) {
				case ADD:
					observer.added(obj);
					break;
				case REMOVE:
					observer.removed(obj);
					break;
				case UPDATE:
					observer.updated(obj);
					break;
				case RESET:
					observer.reset();
					break;
			}
		}
	}
}
