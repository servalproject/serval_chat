package org.servalproject.servalchat.views;

import android.os.Bundle;
import android.view.View;

import org.servalproject.mid.Identity;
import org.servalproject.servalchat.navigation.ILifecycle;

/**
 * Created by jeremy on 20/07/16.
 */
public abstract class Presenter<V extends View> implements ILifecycle {
	private V view;

	public final String key;
	public final Identity identity;
	private PresenterFactory<V, ?> factory;

	protected Presenter(PresenterFactory<V, ?> factory, String key, Identity identity) {
		this.key = key;
		this.identity = identity;
		this.factory = factory;
	}

	protected final V getView() {
		return view;
	}

	public final void takeView(V view) {
		this.view = view;
		if (view != null)
			bind();
	}

	protected void bind() {
	}

	protected void save(Bundle config) {

	}

	protected void restore(Bundle config) {

	}

	protected void onDestroy() {

	}

	@Override
	public void onDetach(boolean changingConfig) {
		takeView(null);
		if (!changingConfig) {
			factory.release(this);
			factory = null;
			onDestroy();
		}
	}

	@Override
	public void onVisible() {

	}

	@Override
	public void onHidden() {

	}
}
