package org.servalproject.mid;

import org.servalproject.servaldna.ServalDInterfaceException;

import java.io.IOException;

/**
 * Created by jeremy on 8/08/16.
 */
public interface IObservableList<T, E extends Exception> {
	void observe(ListObserver<T> observer);

	void stopObserving(ListObserver<T> observer);

	T next() throws ServalDInterfaceException, E, IOException;

	void close();
}
