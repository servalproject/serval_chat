package org.servalproject.mid;

import org.servalproject.servaldna.AbstractJsonList;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.SigningKey;
import org.servalproject.servaldna.meshmb.MessagePlyList;
import org.servalproject.servaldna.meshmb.PlyMessage;

import java.io.IOException;

/**
 * Created by jeremy on 3/08/16.
 */
public class MessageFeed extends AbstractGrowingList<PlyMessage, IOException> {
	private final SigningKey id;
	private String token;
	private PlyMessage last;
	private Peer peer;
	private String name;

	MessageFeed(Serval serval, Peer peer) {
		this(serval, peer.getSubscriber().signingKey);
		this.peer = peer;
	}

	MessageFeed(Serval serval, SigningKey id) {
		super(serval);
		if (id == null)
			throw new NullPointerException("A bundle signing key is required");
		this.id = id;
	}

	@Override
	protected void start() {
		if (token == null)
			return;
		super.start();
	}

	@Override
	protected AbstractJsonList<PlyMessage, IOException> openPast() throws ServalDInterfaceException, IOException {
		MessagePlyList list = serval.getResultClient().meshmbListMessages(id);
		this.name = list.getName();
		if (peer != null)
			peer.updateFeedName(name);
		return list;
	}

	@Override
	protected AbstractJsonList<PlyMessage, IOException> openFuture() throws ServalDInterfaceException, IOException {
		MessagePlyList list = serval.getResultClient().meshmbListMessagesSince(id, token);
		this.name = list.getName();
		if (peer != null)
			peer.updateFeedName(name);
		return list;
	}

	@Override
	protected void addingFutureItem(PlyMessage item) {
		if (last == null || last.compareTo(item)<0) {
			last = item;
			token = item.token;
		}
		super.addingFutureItem(item);
	}

	@Override
	protected void addingPastItem(PlyMessage item) {
		if (token == null) {
			last = item;
			token = (item == null) ? "" : item.token;
			start();
		}
		super.addingPastItem(item);
	}

}
