package org.servalproject.servalchat.views;

import android.os.Bundle;
import android.view.View;

import org.servalproject.mid.Identity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jeremy on 20/07/16.
 */
public abstract class PresenterFactory<V extends View, P extends Presenter<V>> {

	private Map<String, P> presenters = new HashMap<>();

	protected String getKey(V view, Identity id, Bundle savedState) {
		return id == null ? "null" : id.subscriber.toString();
	}

	public final P getPresenter(V view, Identity id, Bundle savedState) {
		String key = getKey(view, id, savedState);
		P ret = presenters.get(key);
		if (ret == null) {
			ret = create(key, id);
			ret.restore(savedState);
		}
		ret.takeView(view);
		presenters.put(key, ret);
		return ret;
	}

	public void release(Presenter<?> presenter) {
		presenters.remove(presenter.key);
	}

	protected abstract P create(String key, Identity id);
}
