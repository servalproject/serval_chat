package org.servalproject.mid;

public interface IObserverSet<T> {
	void addUI(Observer<T> observer);

	void removeUI(Observer<T> observer);

	void addBackground(Observer<T> observer);

	void removeBackground(Observer<T> observer);

	T getObj();
}
