package org.servalproject.mid;

import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.SigningKey;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.SubscriberId;
import org.servalproject.servaldna.meshmb.MeshMBCommon;
import org.servalproject.servaldna.meshmb.MeshMBSubscription;
import org.servalproject.servaldna.meshmb.MeshMBSubscriptionList;
import org.servalproject.servaldna.meshms.MeshMSConversation;
import org.servalproject.servaldna.meshms.MeshMSConversationList;
import org.servalproject.servaldna.meshms.MeshMSException;
import org.servalproject.servaldna.rhizome.RhizomeListBundle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by jeremy on 11/07/16.
 */
public class Messaging {
	private final Serval serval;
	private static final String SERVICE = "MeshMS2";
	private final Identity identity;
	private int hashCode;
	private int unreadCount;
	private int requestsUnread;
	private boolean loadedSubscriptions = false;
	private RhizomeListBundle last;
	private static final String TAG = "Messaging";

	public final List<MeshMSConversation> conversations = new ArrayList<>();
	public final List<MeshMSConversation> requests = new ArrayList<>();
	public final List<Peer> contacts = new ArrayList<>();

	private final HashMap<SubscriberId, MeshMSConversation> hashmap = new HashMap<>();
	private final Set<Subscriber> following = new HashSet<>();
	private final Set<SubscriberId> followingSids = new HashSet<>();
	private final Set<Subscriber> blocking = new HashSet<>();
	private final Set<SubscriberId> blockingSids = new HashSet<>();

	public final ListObserverSet<MeshMSConversation> observers;
	public final ListObserverSet<Peer> observeContacts;
	public final ListObserverSet<Peer> observeBlockList;

	public int getHashCode() {
		return hashCode;
	}

	public int getUnreadCount() {
		return unreadCount;
	}

	public int getUnreadRequests(){
		return requestsUnread;
	}

	Messaging(Serval serval, Identity identity) {
		this.serval = serval;
		this.identity = identity;
		this.observers = new ListObserverSet<>(serval);
		this.observeContacts = new ListObserverSet<>(serval);
		this.observeBlockList = new ListObserverSet<>(serval);

		// TODO add restful api for conversation list updates?
		serval.rhizome.observerSet.addBackground(rhizomeObserver);
		serval.rhizome.observers.addBackground(new Observer<Rhizome>() {
			@Override
			public void updated(Rhizome obj) {
				refresh();
			}
		});
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
		if (!serval.rhizome.isEnabled())
			return;
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
			if (subscription.blocked){
				blocking.add(subscription.subscriber);
				blockingSids.add(subscription.subscriber.sid);
				continue;
			}
			following.add(subscription.subscriber);
			followingSids.add(subscription.subscriber.sid);
			Peer p = serval.knownPeers.getPeer(subscription.subscriber);
			this.contacts.add(p);
			// Don't overwrite the feed name with a cached name that might be stale
			if (p.getFeedName()==null && subscription.name!=null)
				p.updateFeedName(subscription.name);
		}
		observeContacts.onReset();
		loadedSubscriptions = true;
	}

	public List<Peer> getBlockList(){
		List<Peer> ret = new ArrayList<>();
		for (Subscriber k: blocking) {
			ret.add(serval.knownPeers.getPeer(k));
		}
		return ret;
	}

	public enum SubscriptionState{
		Followed,
		Ignored,
		Blocked
	}

	public SubscriptionState getSubscriptionState(Subscriber id){
		if (following.contains(id))
			return SubscriptionState.Followed;
		if (blocking.contains(id))
			return SubscriptionState.Blocked;
		return SubscriptionState.Ignored;
	}
/*
	public SubscriptionState getSubscriptionState(SubscriberId subscriber){
		if (!subscriptionsBySid.containsKey(subscriber))
			return SubscriptionState.Ignored;
		return subscriptionsBySid.get(subscriber);
	}
*/
	void subscriptionAltered(MeshMBCommon.SubscriptionAction action, Peer peer){
		Subscriber id = peer.getSubscriber();

		switch (action){
			case Follow:
				if (following.contains(id))
					return;
				following.add(id);
				followingSids.add(id.sid);
				if (blocking.remove(id)){
					blockingSids.remove(id.sid);
					observeBlockList.onRemove(peer);
				}
				contacts.add(peer);
				observeContacts.onAdd(peer);
				break;
			case Ignore:
				if (blocking.remove(id)){
					blockingSids.remove(id.sid);
					observeBlockList.onRemove(peer);
				}
				followingSids.remove(id.sid);
				if (following.remove(id)) {
					contacts.remove(peer);
					observeContacts.onRemove(peer);
				}
				break;
			case Block:
				if (blocking.contains(id))
					return;
				followingSids.remove(id.sid);
				blocking.add(id);
				blockingSids.add(id.sid);
				observeBlockList.onAdd(peer);
				if (following.remove(id)){
					contacts.remove(peer);
					observeContacts.onRemove(peer);
				}
				break;
		}
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
				int requestsUnread = 0;

				try {
					MeshMSConversation conversation;
					while ((conversation = list.nextConversation()) != null) {
						if (cancel)
							return;
						if (!conversation.isRead) {
							if (followingSids.contains(conversation.them.sid)){
								hashCode = hashCode ^ conversation.readHashCode();
								unreadCount++;
							}else if(!blockingSids.contains(conversation.them.sid)) {
								requestsUnread++;
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
					for(MeshMSConversation c:replace) {
						hashmap.put(c.them.sid, c);
						if (followingSids.contains(c.them.sid)){
							conversations.add(c);
						}else if(!blockingSids.contains(c.them.sid)) {
							requests.add(c);
						}
					}
					Messaging.this.hashCode = hashCode;
					Messaging.this.unreadCount = unreadCount;
					Messaging.this.requestsUnread = requestsUnread;
				}
				observers.onReset();
			} catch (ServalDInterfaceException |
					MeshMSException |
					IOException e) {
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
