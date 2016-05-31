package org.servalproject.mid;

import android.util.Log;

import org.servalproject.servaldna.AsyncResult;
import org.servalproject.servaldna.MdpDnaLookup;
import org.servalproject.servaldna.MdpRoutingChanges;
import org.servalproject.servaldna.RouteLink;
import org.servalproject.servaldna.ServalDCommand;
import org.servalproject.servaldna.SubscriberId;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jeremy on 4/05/16.
 */
public class KnownPeers {

	private static final String TAG = "Peers";
	private MdpDnaLookup dnaLookup;
	private MdpRoutingChanges routingChanges;
	private final Serval serval;
	private final Map<SubscriberId, Peer> peers = new HashMap<SubscriberId, Peer>();
	private int reachableCount=0;
	public final ListObserverSet<Peer> peerListObservers;
	public final ObserverSet<KnownPeers> observers;

	KnownPeers(Serval serval){
		this.serval = serval;
		peerListObservers = new ListObserverSet<>(serval.uiHandler);
		observers = new ObserverSet<>(serval.uiHandler, this);
	}

	public int getReachableCount(){
		return reachableCount;
	}

	public Collection<Peer> getKnownPeers(){
		return peers.values();
	}

	private final AsyncResult<ServalDCommand.LookupResult> dnaResults = new AsyncResult<ServalDCommand.LookupResult>() {
		@Override
		public void result(ServalDCommand.LookupResult nextResult) {
			Peer p = getPeer(nextResult.subscriberId);
			p.update(nextResult);
		}
	};

	private Peer getPeer(SubscriberId sid){
		Peer p = peers.get(sid);
		if (p!=null)
			return p;

		synchronized (this){
			p = peers.get(sid);
			if (p!=null)
				return p;
			p = new Peer(serval.uiHandler, sid);
			peers.put(sid, p);
		}
		peerListObservers.onAdd(p);
		return p;
	}

	private final AsyncResult<RouteLink> routeResults = new AsyncResult<RouteLink>() {
		@Override
		public void result(RouteLink nextResult) {
			if (nextResult.isSelf())
				return;
			Peer p = getPeer(nextResult.sid);
			boolean wasReachable = p.isReachable();
			p.update(nextResult);
			boolean nowReachable = p.isReachable();
			if (nowReachable != wasReachable){
				if (nowReachable)
					reachableCount++;
				else
					reachableCount--;
				observers.onUpdate();
			}
			if (p.isReachable() && p.lookup==null)
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

	public void requestRefresh(Peer p){
		try {
			dnaLookup.sendRequest(p.sid, "");
		} catch (IOException e) {
			// We might as well crash, something has gone terribly wrong
			throw new IllegalStateException(e);
		}
	}
}
