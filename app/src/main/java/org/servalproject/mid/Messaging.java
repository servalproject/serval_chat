package org.servalproject.mid;

import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.SubscriberId;
import org.servalproject.servaldna.meshmb.MeshMBSubscription;
import org.servalproject.servaldna.meshmb.MeshMBSubscriptionList;
import org.servalproject.servaldna.meshms.MeshMSConversation;
import org.servalproject.servaldna.meshms.MeshMSConversationList;
import org.servalproject.servaldna.rhizome.RhizomeListBundle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jeremy on 11/07/16.
 */
public class Messaging {
	private final Serval serval;
	private static final String SERVICE = "MeshMS2";
	private final Identity identity;
	private int hashCode;
	private int unreadCount;
	private boolean loadedSubscriptions = false;
	private RhizomeListBundle last;
	private static final String TAG = "Messaging";

	public final List<MeshMSConversation> conversations = new ArrayList<>();
	public final List<MeshMSConversation> requests = new ArrayList<>();
	public final List<MeshMSConversation> blocked = new ArrayList<>();
	private final HashMap<SubscriberId, MeshMSConversation> hashmap = new HashMap<>();
	private final HashMap<Subscriber, SubscriptionState> subscriptions = new HashMap<>();
	private final HashMap<SubscriberId, SubscriptionState> subscriptionsBySid = new HashMap<>();
	public final ListObserverSet<MeshMSConversation> observers;

	public int getHashCode() {
		return hashCode;
	}

	public int getUnreadCount() {
		return unreadCount;
	}

	Messaging(Serval serval, Identity identity) {
		this.serval = serval;
		this.identity = identity;
		this.observers = new ListObserverSet<>(serval);

		// TODO add restful api for conversation list updates?
		serval.rhizome.observerSet.addBackground(rhizomeObserver);
		refresh();
	}

	private final ListObserver<RhizomeListBundle> rhizomeObserver = new ListObserver<RhizomeListBundle>() {
		@Override
		public void added(RhizomeListBundle obj) {
			if (obj.manifest.service.equals(SERVICE)
					&& obj.manifest.recipient.equals(identity.subscriber.sid)
					&& (last == null || last.compareTo(obj) < 0)) {
				last = obj;
				refresh();
			}
		}

		@Override
		public void removed(RhizomeListBundle obj) {
		}

		@Override
		public void updated(RhizomeListBundle obj) {
		}

		@Override
		public void reset() {
		}
	};

	private Refresh refreshing;

	void refresh() {
		synchronized (this) {
			Refresh refreshing = this.refreshing;
			if (refreshing != null)
				refreshing.cancel = true;
			this.refreshing = new Refresh();
		}
		serval.runOnThreadPool(refreshing);
	}

	private void loadSubscriptions() throws IOException, ServalDInterfaceException {
		MeshMBSubscriptionList subscriptions = serval.getResultClient().meshmbSubscriptions(identity.subscriber);
		MeshMBSubscription subscription;
		while((subscription = subscriptions.next())!=null){
			SubscriptionState state = subscription.blocked ? SubscriptionState.Blocked : SubscriptionState.Followed;
			this.subscriptions.put(subscription.subscriber, state);
			this.subscriptionsBySid.put(subscription.subscriber.sid, state);
			Peer p = serval.knownPeers.getPeer(subscription.subscriber);
			// Don't overwrite the feed name with a cached name that might be stale
			if (p.getFeedName()==null)
				p.updateFeedName(subscription.name);
		}
		loadedSubscriptions = true;
	}

	public enum SubscriptionState{
		Followed,
		Ignored,
		Blocked
	}

	public SubscriptionState getSubscriptionState(Subscriber subscriber){
		if (!subscriptions.containsKey(subscriber))
			return SubscriptionState.Ignored;
		return subscriptions.get(subscriber);
	}

	public SubscriptionState getSubscriptionState(SubscriberId subscriber){
		if (!subscriptionsBySid.containsKey(subscriber))
			return SubscriptionState.Ignored;
		return subscriptionsBySid.get(subscriber);
	}

	private void putSubscription(MessageFeed feed, boolean blocked){
		SubscriptionState state = blocked ? SubscriptionState.Blocked : SubscriptionState.Followed;
		subscriptions.put(feed.getId(), state);
		subscriptionsBySid.put(feed.getId().sid, state);
		refresh();
	}

	void blocked(MessageFeed feed){
		putSubscription(feed, true);
	}

	void followed(MessageFeed feed){
		putSubscription(feed, false);
	}

	void ignored(MessageFeed feed){
		subscriptions.remove(feed.getId());
		subscriptionsBySid.remove(feed.getId().sid);
		refresh();
	}

	private class Refresh implements Runnable {
		private boolean cancel = false;

		@Override
		public void run() {
			try {
				if (!loadedSubscriptions)
					loadSubscriptions();

				if (cancel)
					return;

				// TODO abort on new incoming message?
				List<MeshMSConversation> replace = new ArrayList<>();
				MeshMSConversationList list = serval.getResultClient().meshmsListConversations(identity.subscriber.sid);
				int hashCode = 0;
				int unreadCount = 0;

				try {
					MeshMSConversation conversation;
					while ((conversation = list.nextConversation()) != null) {
						if (cancel)
							return;
						if (!conversation.isRead) {
							if (getSubscriptionState(conversation.them.sid) == SubscriptionState.Followed){
								hashCode = hashCode ^ conversation.readHashCode();
								unreadCount++;
							}
						}
						replace.add(conversation);
					}
				} finally {
					list.close();
				}

				synchronized (Messaging.this) {
					if (cancel)
						return;
					conversations.clear();
					requests.clear();
					blocked.clear();
					for(MeshMSConversation c:replace) {
						hashmap.put(c.them.sid, c);
						switch (getSubscriptionState(c.them.sid)){
							case Followed:
								conversations.add(c);
								break;
							case Ignored:
								requests.add(c);
								break;
							case Blocked:
								blocked.add(c);
								break;
						}
					}
					Messaging.this.hashCode = hashCode;
					Messaging.this.unreadCount = unreadCount;
				}
				observers.onReset();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			} finally {
				synchronized (Messaging.this) {
					if (refreshing == this)
						refreshing = null;
				}
			}
		}
	}

	public MeshMSConversation getPrivateConversation(Subscriber peer){
		return hashmap.get(peer.sid);
	}

	public MessageList getPrivateMessages(Subscriber peer) {
		return new MessageList(serval, this, identity.subscriber, peer);
	}
}
