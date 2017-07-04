package org.servalproject.servalchat.views;

import android.os.Bundle;
import android.view.View;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Peer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jeremy on 20/07/16.
 */
public abstract class PresenterFactory<V extends View, P extends Presenter<V>> {

	private Map<String, P> presenters = new HashMap<>();

	protected String getKey(V view, Identity id, Peer peer, Bundle savedState) {
		if (id == null)
			return "null";
		if (peer == null)
			return id.subscriber.signingKey.toHex();
		return id.subscriber.signingKey.toHex() + peer.getSubscriber().sid.toHex();
	}

	public final P getPresenter(V view, Identity id, Peer peer, Bundle savedState) {
		String key = getKey(view, id, peer, savedState);
		P ret = presenters.get(key);
		if (ret == null) {
			ret = create(key, id, peer);
			ret.restore(savedState);
		}
		ret.takeView(view);
		presenters.put(key, ret);
		return ret;
	}

	public void release(Presenter<?> presenter) {
		presenters.remove(presenter.key);
	}

	protected abstract P create(String key, Identity id, Peer peer);
}
