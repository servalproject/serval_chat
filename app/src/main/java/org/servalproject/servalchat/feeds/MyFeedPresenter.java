package org.servalproject.servalchat.feeds;

import android.os.Bundle;

import org.servalproject.mid.Identity;
import org.servalproject.mid.IdentityFeed;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.App;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.BackgroundWorker;
import org.servalproject.servalchat.views.Presenter;
import org.servalproject.servalchat.views.PresenterFactory;
import org.servalproject.servaldna.Subscriber;

/**
 * Created by jeremy on 8/08/16.
 */
public class MyFeedPresenter extends Presenter<MyFeed> {
	private final IdentityFeed feed;
	private ActivityAdapter adapter;

	protected MyFeedPresenter(PresenterFactory<MyFeed, ?> factory, String key, Identity identity) {
		super(factory, key, identity);
		this.feed = identity.getFeed();
	}

	public static final PresenterFactory<MyFeed, MyFeedPresenter> factory = new PresenterFactory<MyFeed, MyFeedPresenter>() {
		@Override
		protected MyFeedPresenter create(String key, Identity id, Peer peer) {
			return new MyFeedPresenter(this, key, id);
		}
	};

	private boolean posting = false;

	private void setEnabled() {
		MyFeed view = getView();
		if (view == null)
			return;
		view.post.setEnabled(!posting);
		view.message.setEnabled(!posting);
	}

	@Override
	protected void restore(Bundle config) {
		super.restore(config);
		adapter = new ActivityAdapter(identity.getActivity(), this);
	}

	@Override
	protected void bind(MyFeed view) {
		view.list.setAdapter(adapter);
		setEnabled();
		if (App.isTesting() && "".equals(view.message.getText().toString()))
			view.message.setText("Sample Post \uD83D\uDE00");
	}

	public void post() {
		MyFeed view = getView();
		if (view == null)
			return;
		final String message = view.message.getText().toString();
		if ("".equals(message))
			return;

		posting = true;
		setEnabled();
		new BackgroundWorker() {
			@Override
			protected void onBackGround() throws Exception {
				feed.sendMessage(message);
			}

			@Override
			protected void onComplete(Throwable t) {
				posting = false;
				MyFeed view = getView();
				if (view != null) {
					if (t == null)
						view.message.setText("");
					else
						view.activity.showError(t);
					setEnabled();
				}else
					rethrow(t);
			}
		}.execute();
	}

	public void openFeed(Subscriber subscriber, long offset) {
		MyFeed view = getView();
		if (view == null || subscriber.equals(identity.subscriber))
			return;
		Peer peer = Serval.getInstance().knownPeers.getPeer(subscriber);
		Bundle args = new Bundle();
		args.putLong("offset", offset);
		view.activity.go(Navigation.PeerFeed, peer, args);
	}

	protected MainActivity getActivity() {
		MyFeed view = getView();
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
}
