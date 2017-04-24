package org.servalproject.mid;

import org.servalproject.servaldna.AbstractJsonList;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.SigningKey;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.meshmb.MeshMBSubscription;
import org.servalproject.servaldna.meshmb.MessagePlyList;
import org.servalproject.servaldna.meshmb.PlyMessage;

import java.io.IOException;

/**
 * Created by jeremy on 3/08/16.
 */
public class MessageFeed extends AbstractFutureList<PlyMessage, IOException> {
	public final Subscriber id;
	private Peer peer;
	private String name;

	MessageFeed(Serval serval, Peer peer) {
		this(serval, peer.getSubscriber());
		this.peer = peer;
	}

	MessageFeed(Serval serval, Subscriber id) {
		super(serval);
		if (id == null || id.signingKey == null)
			throw new NullPointerException("A bundle signing key is required");
		this.id = id;
	}

	@Override
	protected void start() {
		if (last == null && hasMore)
			return;
		super.start();
	}

	@Override
	protected AbstractJsonList<PlyMessage, IOException> openPast() throws ServalDInterfaceException, IOException {
		MessagePlyList list = serval.getResultClient().meshmbListMessages(id.signingKey);
		this.name = list.getName();
		if (peer != null)
			peer.updateFeedName(name);
		return list;
	}

	@Override
	protected AbstractJsonList<PlyMessage, IOException> openFuture() throws ServalDInterfaceException, IOException {
		MessagePlyList list = serval.getResultClient().meshmbListMessagesSince(id.signingKey, last==null?"":last.token);
		this.name = list.getName();
		if (peer != null)
			peer.updateFeedName(name);
		return list;
	}
}
