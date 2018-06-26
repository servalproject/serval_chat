package org.servalproject.mid;

import org.servalproject.servaldna.HttpJsonSerialiser;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.meshms.MeshMSConversation;
import org.servalproject.servaldna.meshms.MeshMSException;
import org.servalproject.servaldna.meshms.MeshMSMessage;

import java.io.IOException;

/**
 * Created by jeremy on 11/07/16.
 */
public class MessageList extends AbstractFutureList<MeshMSMessage, ServalDInterfaceException> {
	private final Messaging messaging;
	public final Subscriber self;
	public final Subscriber peer;

	MessageList(Serval serval, Messaging messaging, Subscriber self, Subscriber peer) {
		super(serval);
		this.messaging = messaging;
		this.self = self;
		this.peer = peer;
	}

	@Override
	protected void start() {
		if (last == null && hasMore)
			return;
		super.start();
	}

	@Override
	protected HttpJsonSerialiser<MeshMSMessage, ServalDInterfaceException> openPast() throws ServalDInterfaceException, IOException {
		return serval.getResultClient().meshmsListMessages(self.sid, peer.sid);
	}

	@Override
	protected HttpJsonSerialiser<MeshMSMessage, ServalDInterfaceException> openFuture() throws ServalDInterfaceException, IOException {
		return serval.getResultClient().meshmsListMessagesSince(self.sid, peer.sid, last==null?"":last.token);
	}

	public void sendMessage(String message) throws ServalDInterfaceException, MeshMSException, IOException {
		if (serval.uiHandler.isOnThread())
			throw new IllegalStateException();
		serval.getResultClient().meshmsSendMessage(self.sid, peer.sid, message);
	}

	public boolean isRead(){
		MeshMSConversation conv = messaging.getPrivateConversation(peer);
		return conv == null || conv.isRead;
	}

	public void markRead() throws ServalDInterfaceException, MeshMSException, IOException {
		if (serval.uiHandler.isOnThread())
			throw new IllegalStateException();
		serval.getResultClient().meshmsMarkAllMessagesRead(self.sid, peer.sid);
		messaging.refresh();
	}
}
