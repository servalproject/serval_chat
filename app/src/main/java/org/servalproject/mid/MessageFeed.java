package org.servalproject.mid;

import org.servalproject.servaldna.AbstractJsonList;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.SigningKey;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.meshmb.MeshMBCommon;
import org.servalproject.servaldna.meshmb.MessagePlyList;
import org.servalproject.servaldna.meshmb.PlyMessage;
import org.servalproject.servaldna.rhizome.RhizomeBundleList;
import org.servalproject.servaldna.rhizome.RhizomeListBundle;

import java.io.IOException;

/**
 * Created by jeremy on 3/08/16.
 */
public class MessageFeed extends AbstractFutureList<PlyMessage, IOException> {
	private Subscriber id;
	private Peer peer;
	private String name;

	MessageFeed(Serval serval, Peer peer) {
		super(serval);
		this.peer = peer;
		Subscriber peerId = peer.getSubscriber();
		if (peerId.signingKey!=null)
			this.id = peerId;
	}

	MessageFeed(Serval serval, Subscriber id) {
		super(serval);
		if (id == null || id.signingKey == null)
			throw new IllegalStateException();
		this.id = id;
	}

	public Peer getPeer(){
		return peer;
	}

	public Subscriber getId(){
		if (id == null && peer != null){
			// might have discovered the key some other way in the mean time
			Subscriber peerId = peer.getSubscriber();
			if (peerId.signingKey != null)
				id = peerId;
		}
		return id;
	}

	@Override
	protected void start() {
		if (last == null && hasMore)
			return;
		super.start();
	}

	private void findKey() throws IOException, ServalDInterfaceException {
		if (id!=null)
			return;
		if (peer == null)
			throw new IllegalStateException();

		// might have discovered the key some other way in the mean time
		Subscriber peerId = peer.getSubscriber();
		if (peerId.signingKey != null){
			id = peerId;
			return;
		}
		// look for a feed in rhizome
		RhizomeBundleList list = new RhizomeBundleList(serval.getResultClient());
		try {
			list.setServiceFilter(MeshMBCommon.SERVICE);
			list.setSenderFilter(peer.getSubscriber().sid);
			list.connect();
			RhizomeListBundle bundle = list.next();
			if (bundle != null){
				id = new Subscriber(peerId.sid, bundle.manifest.id, true);
				peer.updateSubscriber(id);
			}
		}finally {
			list.close();
		}
	}

	@Override
	protected AbstractJsonList<PlyMessage, IOException> openPast() throws ServalDInterfaceException, IOException {
		findKey();
		if (id == null)
			return null;
		MessagePlyList list = serval.getResultClient().meshmbListMessages(id.signingKey);
		this.name = list.getName();
		if (peer != null)
			peer.updateFeedName(name);
		return list;
	}

	@Override
	protected AbstractJsonList<PlyMessage, IOException> openFuture() throws ServalDInterfaceException, IOException {
		findKey();
		if (id == null)
			return null;
		MessagePlyList list = serval.getResultClient().meshmbListMessagesSince(id.signingKey, last==null?"":last.token);
		this.name = list.getName();
		if (peer != null)
			peer.updateFeedName(name);
		return list;
	}
}
