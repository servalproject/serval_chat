package org.servalproject.servalchat.feeds;

import android.os.Bundle;

import org.servalproject.mid.Identity;
import org.servalproject.mid.KnownPeers;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.Presenter;
import org.servalproject.servalchat.views.PresenterFactory;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.Subscriber;

import java.io.IOException;

/**
 * Created by jeremy on 30/01/17.
 */

public class ActivityPresenter extends Presenter<Activity> {
	ActivityAdapter adapter;
	protected ActivityPresenter(PresenterFactory<Activity, ?> factory, String key, Identity identity) {
		super(factory, key, identity);
	}

	public static PresenterFactory<Activity, ActivityPresenter> factory
			= new PresenterFactory<Activity, ActivityPresenter>() {

		@Override
		protected ActivityPresenter create(String key, Identity id) {
			return new ActivityPresenter(this, key, id);
		}
	};

	@Override
	protected void bind() {
		getView().setAdapter(adapter);
		super.bind();
	}

	@Override
	public void onVisible() {
		super.onVisible();
		adapter.onVisible();
	}

	@Override
	protected void restore(Bundle config) {
		adapter = new ActivityAdapter(identity.getActivity(), this);
	}

	@Override
	public void onHidden() {
		super.onHidden();
		adapter.onHidden();
	}

	public void openFeed(Subscriber subscriber, long offset) {
		Activity list = getView();
		if (list == null)
			return;
		Bundle args = new Bundle();
		args.putLong("offset", offset);
		KnownPeers.saveSubscriber(subscriber, args);
		list.activity.go(identity, Navigation.PeerFeed, args);
	}
}
