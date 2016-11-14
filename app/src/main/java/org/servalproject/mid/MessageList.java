package org.servalproject.mid;

import org.servalproject.servaldna.AbstractJsonList;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.meshms.MeshMSConversation;
import org.servalproject.servaldna.meshms.MeshMSException;
import org.servalproject.servaldna.meshms.MeshMSMessage;

import java.io.IOException;

/**
 * Created by jeremy on 11/07/16.
 */
public class MessageList extends AbstractGrowingList<MeshMSMessage, MeshMSException> {
	private final Messaging messaging;
	public final Subscriber self;
	public final Subscriber peer;
	private String token;
	private MeshMSMessage last;

	MessageList(Serval serval, Messaging messaging, Subscriber self, Subscriber peer) {
		super(serval);
		this.messaging = messaging;
		this.self = self;
		this.peer = peer;
	}

	@Override
	protected void start() {
		if (token == null)
			return;
		super.start();
	}

	@Override
	protected AbstractJsonList<MeshMSMessage, MeshMSException> openPast() throws ServalDInterfaceException, MeshMSException, IOException {
		return serval.getResultClient().meshmsListMessages(self.sid, peer.sid);
	}

	@Override
	protected AbstractJsonList<MeshMSMessage, MeshMSException> openFuture() throws ServalDInterfaceException, MeshMSException, IOException {
		return serval.getResultClient().meshmsListMessagesSince(self.sid, peer.sid, token);
	}

	@Override
	protected void addingFutureItem(MeshMSMessage item) {
		if (last == null || last.compareTo(item)<0) {
			last = item;
			token = item.token;
		}
		super.addingFutureItem(item);
	}

	@Override
	protected void addingPastItem(MeshMSMessage item) {
		if (last == null) {
			last = item;
			token = (item == null) ? "" : item.token;
			start();
		}
		super.addingPastItem(item);
	}

	public void sendMessage(String message) throws ServalDInterfaceException, MeshMSException, IOException {
		if (serval.uiHandler.isUiThread())
			throw new IllegalStateException();
		serval.getResultClient().meshmsSendMessage(self.sid, peer.sid, message);
	}

	public boolean isRead(){
		MeshMSConversation conv = messaging.getPrivateConversation(peer);
		return conv == null || conv.isRead;
	}

	public void markRead() throws ServalDInterfaceException, MeshMSException, IOException {
		if (serval.uiHandler.isUiThread())
			throw new IllegalStateException();
		serval.getResultClient().meshmsMarkAllMessagesRead(self.sid, peer.sid);
		messaging.refresh();
	}
}
