package org.servalproject.servalchat.views;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import org.servalproject.mid.Identity;
import org.servalproject.mid.ListObserver;
import org.servalproject.mid.ListObserverSet;
import org.servalproject.mid.Peer;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;

/**
 * Created by jeremy on 20/06/16.
 */
public abstract class ObservedRecyclerView<T, H extends RecyclerView.ViewHolder>
		extends SimpleRecyclerView<T, H>
		implements ILifecycle, INavigate, ListObserver<T> {

	protected MainActivity activity;
	protected Identity identity;
	protected Peer peer;
	private ListObserverSet<T> observerSet;
	private int generation = -1;

	@Override
	public void added(T obj) {
		notifyChanged();
	}

	@Override
	public void removed(T obj) {
		notifyChanged();
	}

	@Override
	public void updated(T obj) {
		notifyChanged();
	}

	@Override
	public void reset() {
		notifyChanged();
	}

	public ObservedRecyclerView(Context context, @Nullable AttributeSet attrs) {
		this(null, context, attrs);
	}

	public ObservedRecyclerView(ListObserverSet<T> observerSet, Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		this.observerSet = observerSet;
	}

	public void setObserverSet(ListObserverSet<T> observerSet) {
		this.observerSet = observerSet;
	}

	@Override
	public void onVisible() {
		if (observerSet != null) {
			int g = observerSet.add(this);
			if (g != generation)
				notifyChanged();
		}
	}

	@Override
	public void onHidden() {
		if (observerSet != null)
			generation = observerSet.remove(this);
	}

	@Override
	public void onDetach(boolean configChanging) {

	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args) {
		this.activity = activity;
		this.identity = id;
		this.peer = peer;
		return this;
	}

}
