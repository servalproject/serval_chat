package org.servalproject.servalchat.feeds;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;

import org.servalproject.mid.Identity;
import org.servalproject.mid.KnownPeers;
import org.servalproject.mid.MessageFeed;
import org.servalproject.mid.Messaging;
import org.servalproject.mid.Observer;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.views.Presenter;
import org.servalproject.servalchat.views.PresenterFactory;
import org.servalproject.servaldna.AbstractId;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.meshmb.MeshMBCommon;
import org.servalproject.servaldna.meshmb.MeshMBSubscription;

/**
 * Created by jeremy on 3/08/16.
 */
public class PeerFeedPresenter extends Presenter<PeerFeed> {

	private FeedAdapter adapter;
	private final Peer peer;
	private MessageFeed feed;

	private Observer<Peer> peerObserver = new Observer<Peer>() {
		@Override
		public void updated(Peer obj) {
			// if we discover a peer signing key, reset our adapter
			if (obj.getSubscriber().signingKey != null && (feed == null || feed.getId() == null)){
				feed = peer.getFeed();
				adapter = new FeedAdapter(feed);
				PeerFeed view = getView();
				if (view != null) {
					view.list.setAdapter(adapter);
					view.activity.supportInvalidateOptionsMenu();
				}
			}
		}
	};

	protected PeerFeedPresenter(PresenterFactory<PeerFeed, ?> factory, String key, Identity identity, Peer peer) {
		super(factory, key, identity);
		this.peer = peer;
	}

	public static PresenterFactory<PeerFeed, PeerFeedPresenter> factory
			= new PresenterFactory<PeerFeed, PeerFeedPresenter>() {

		@Override
		protected PeerFeedPresenter create(String key, Identity id, Peer peer) {
			return new PeerFeedPresenter(this, key, id, peer);
		}

	};

	@Override
	protected void bind(PeerFeed feed) {
		feed.list.setAdapter(adapter);
	}

	@Override
	protected void restore(Bundle config) {
		feed = peer.getFeed();
		adapter = new FeedAdapter(feed);
	}

	@Override
	public void onVisible() {
		super.onVisible();
		adapter.onVisible();
		peer.observers.addUI(this.peerObserver);
	}

	@Override
	public void onHidden() {
		super.onHidden();
		adapter.onHidden();
		peer.observers.removeUI(this.peerObserver);
	}
}
