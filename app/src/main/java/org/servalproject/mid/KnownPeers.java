package org.servalproject.mid;

import android.os.Bundle;

import org.servalproject.servaldna.AbstractId;
import org.servalproject.servaldna.AsyncResult;
import org.servalproject.servaldna.MdpDnaLookup;
import org.servalproject.servaldna.MdpRoutingChanges;
import org.servalproject.servaldna.RouteLink;
import org.servalproject.servaldna.ServalDCommand;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.SubscriberId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jeremy on 4/05/16.
 */
public class KnownPeers {

	private MdpDnaLookup dnaLookup;
	private MdpRoutingChanges routingChanges;
	private final Serval serval;
	private final Map<SubscriberId, Peer> peersBySid = new HashMap<>();
	private int reachableCount = 0;
	private Interface interfaces[] = new Interface[16];
	private int interfaceCount=0;
	public final ListObserverSet<Peer> peerListObservers;
	public final ListObserverSet<Interface> interfaceObservers;
	public final ObserverSet<KnownPeers> observers;

	KnownPeers(Serval serval) {
		this.serval = serval;
		peerListObservers = new ListObserverSet<>(serval);
		interfaceObservers = new ListObserverSet<>(serval);
		observers = new ObserverSet<>(serval, this);
	}

	public int getReachableCount() {
		return reachableCount;
	}

	public List<Peer> getReachablePeers() {
		List<Peer> list = new ArrayList<>();
		for (Peer p : peersBySid.values()) {
			if (p.isReachable())
				list.add(p);
		}
		return list;
	}

	private final AsyncResult<ServalDCommand.LookupResult> dnaResults = new AsyncResult<ServalDCommand.LookupResult>() {
		@Override
		public void result(ServalDCommand.LookupResult nextResult) {
			Peer p = getPeer(new Subscriber(nextResult.subscriberId));
			p.update(nextResult);
			peerListObservers.onUpdate(p);
		}
	};

	public static final String THEIR_SID = "Sid";
	public static final String THEIR_SIGN = "Sign";
	public static final String THEIR_COMBINED = "Combined";

	public static void saveSubscriber(Subscriber subscriber, Bundle args) {
		args.putByteArray(THEIR_SID, subscriber.sid.getBinary());
		if (subscriber.signingKey != null)
			args.putByteArray(THEIR_SIGN, subscriber.signingKey.getBinary());
		args.putBoolean(THEIR_COMBINED, subscriber.combined);
	}

	private Peer getPeer(SubscriberId sid){
		if (sid==null)
			return null;
		Peer p = peersBySid.get(sid);
		if (p == null) {
			synchronized (this) {
				p = peersBySid.get(sid);
				if (p == null) {
					p = new Peer(serval, new Subscriber(sid, null, false));
					peersBySid.put(sid, p);
				}
			}
		}
		return p;
	}

	public Peer getPeer(Subscriber subscriber) {
		boolean isNew = false;

		Peer p = peersBySid.get(subscriber.sid);

		if (p == null) {
			synchronized (this) {
				p = peersBySid.get(subscriber.sid);
				if (p == null) {
					p = new Peer(serval, subscriber);
					peersBySid.put(subscriber.sid, p);
					isNew = true;
				}
			}
		}

		if (subscriber.signingKey != null
				&& p.getSubscriber().signingKey == null) {
			p.updateSubscriber(subscriber);
		}

		if (isNew)
			peerListObservers.onAdd(p);

		return p;
	}

	private void interfaceChange(RouteLink link) {
		Interface i = interfaces[link.interface_id];

		if (link.interface_up){
			if (i==null)
				interfaceCount ++;
			else if (i.name.equals(link.interface_name))
				return;
			i = interfaces[link.interface_id] = new Interface(link.interface_id, link.interface_name);
			interfaceObservers.onAdd(i);
		}else if (i!=null){
			interfaces[link.interface_id] = null;
			interfaceCount --;
			interfaceObservers.onRemove(i);
		}
	}

	public List<Interface> getActiveInterfaces(){
		List<Interface> ret = new ArrayList<>();
		for(int i=0;i<interfaces.length;i++){
			if (interfaces[i]!=null)
				ret.add(interfaces[i]);
		}
		return ret;
	}

	public int getInterfaceCount(){
		return interfaceCount;
	}

	private final AsyncResult<RouteLink> routeResults = new AsyncResult<RouteLink>() {
		@Override
		public void result(RouteLink nextResult) {
			if (nextResult.isSelf()) {
				if (nextResult.interface_id >= 0)
					interfaceChange(nextResult);
				return;
			}
			Peer p = getPeer(nextResult.subscriber);
			boolean wasReachable = p.isReachable();

			Peer priorHop = getPeer(nextResult.prior_hop == null?
					nextResult.next_hop : nextResult.prior_hop
			);
			Interface netInterface =
					(nextResult.interface_id>=0 && nextResult.interface_id < interfaces.length) ?
							interfaces[nextResult.interface_id] : null;
			p.update(nextResult, netInterface, priorHop);
			peerListObservers.onUpdate(p);
			boolean nowReachable = p.isReachable();
			if (nowReachable != wasReachable) {
				if (nowReachable)
					reachableCount++;
				else
					reachableCount--;
				observers.onUpdate();
			}
			if (p.isReachable() && p.lookup == null)
				requestRefresh(p);
		}
	};

	void onStart() {
		try {
			dnaLookup = new MdpDnaLookup(serval.selector, serval.server.getMdpPort(), dnaResults);
			routingChanges = new MdpRoutingChanges(serval.selector, serval.server.getMdpPort(), routeResults);
		} catch (IOException e) {
			// Yep, we want to crash (this shouldn't happen, but would completely break everything anyway)
			throw new IllegalStateException(e);
		}
	}

	public void requestRefresh(Peer p) {
		try {
			dnaLookup.sendRequest(p.getSubscriber().sid, "");
		} catch (IOException e) {
			// We might as well crash, something has gone terribly wrong
			throw new IllegalStateException(e);
		}
	}
}
