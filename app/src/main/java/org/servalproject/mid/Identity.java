package org.servalproject.mid;

import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.keyring.KeyringIdentity;
import org.servalproject.servaldna.meshmb.MeshMBCommon;

import java.io.IOException;

/**
 * Created by jeremy on 6/06/16.
 */
public class Identity {
	private final Serval serval;
	public final Subscriber subscriber;
	public final ObserverSet<Identity> observers;
	private KeyringIdentity identity;

	private static long nextId = 0;
	private final long id;
	public final Messaging messaging;

	public Identity(Serval serval, Subscriber subscriber) {
		this.serval = serval;
		this.subscriber = subscriber;
		id = nextId++;
		if (serval == null) {
			// dummy object for ui design
			messaging = null;
			observers = null;
		} else {
			observers = new ObserverSet<>(serval, this);
			this.messaging = new Messaging(serval, this);
		}
	}

	public IdentityFeed getFeed() {
		return new IdentityFeed(serval, this);
	}

	public FeedList getAllFeeds() {
		return new FeedList(serval);
	}

	public ActivityList getActivity() {
		return new ActivityList(serval, this);
	}

	public void alterSubscription(MeshMBCommon.SubscriptionAction action, MessageFeed feed) throws ServalDInterfaceException, IOException {
		serval.getResultClient().meshmbAlterSubscription(subscriber, action, feed.getId(), feed.getName());
		messaging.subscriptionAltered(action, feed);
	}

	public void update(KeyringIdentity id) {
		this.identity = id;
		if (observers != null)
			observers.onUpdate();
	}

	public String getName() {
		return identity == null ? null : identity.name;
	}

	public String getDid() {
		return identity == null ? null : identity.did;
	}

	public long getId() {
		return id;
	}
}
