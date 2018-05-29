package org.servalproject.servalchat.feeds;

import android.os.Bundle;
import android.util.Log;

import org.servalproject.mid.FeedList;
import org.servalproject.mid.Identity;
import org.servalproject.mid.KnownPeers;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.Presenter;
import org.servalproject.servalchat.views.PresenterFactory;
import org.servalproject.servaldna.Subscriber;

/**
 * Created by jeremy on 11/10/16.
 */
public class PublicFeedsPresenter extends Presenter<PublicFeedsList> {
	FeedList list;
	FeedListAdapter adapter;

	private static final String TAG = "PublicFeedsPresenter";

	protected PublicFeedsPresenter(PresenterFactory<PublicFeedsList, ?> factory, String key, Identity identity) {
		super(factory, key, identity);
	}

	public static PresenterFactory<PublicFeedsList, PublicFeedsPresenter> factory
			= new PresenterFactory<PublicFeedsList, PublicFeedsPresenter>() {

		@Override
		protected PublicFeedsPresenter create(String key, Identity id, Peer peer) {
			return new PublicFeedsPresenter(this, key, id);
		}
	};

	@Override
	protected void bind(PublicFeedsList view) {
		view.feedList.setAdapter(adapter);
	}

	@Override
	protected void restore(Bundle config) {
		list = identity.getAllFeeds(null);
		adapter = new FeedListAdapter(list, this);
	}

	public void openFeed(Subscriber subscriber) {
		PublicFeedsList view = getView();
		if (view == null)
			return;
		if (identity.subscriber.equals(subscriber)) {
			view.activity.go(Navigation.MyFeed);
		} else {
			Peer peer = Serval.getInstance().knownPeers.getPeer(subscriber);
			view.activity.go(Navigation.PeerFeed, peer, null);
		}
	}

	protected MainActivity getActivity() {
		PublicFeedsList view = getView();
		return view == null ? null : view.activity;
	}

	@Override
	public void onVisible() {
		super.onVisible();
		adapter.onVisible();
	}

	@Override
	public void onHidden() {
		super.onHidden();
		adapter.onHidden();
	}

	public void search(CharSequence search) {
		String srch = (search == null || search.equals("")) ? null : "%"+search+"%";
		if (list.search == null && srch == null)
			return;
		if (srch != null && srch.equals(list.search))
			return;

		Log.v(TAG, "Replacing adapter for "+srch);
		FeedList list = identity.getAllFeeds(srch);
		FeedListAdapter adapter = new FeedListAdapter(list, this);
		getView().feedList.setAdapter(adapter);
		this.adapter.clear();
		this.list = list;
		this.adapter = adapter;
	}
}
