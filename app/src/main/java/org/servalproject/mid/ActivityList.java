package org.servalproject.mid;

import org.servalproject.servaldna.HttpJsonSerialiser;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.meshmb.MeshMBActivityMessage;

import java.io.IOException;

/**
 * Created by jeremy on 30/01/17.
 */

public class ActivityList extends AbstractFutureList<MeshMBActivityMessage, IOException> {
	private final Identity identity;

	ActivityList(Serval serval, Identity identity) {
		super(serval);
		this.identity = identity;
	}

	@Override
	protected void start() {
		if (last == null && hasMore)
			return;
		super.start();
	}

	private void updatePeer(MeshMBActivityMessage item){
		Peer p = serval.knownPeers.getPeer(item.subscriber);
		p.updateFeedName(item.name);
	}

	@Override
	protected void addingPastItem(MeshMBActivityMessage item) {
		if (item!=null)
			updatePeer(item);
		super.addingPastItem(item);
	}

	@Override
	protected void addingFutureItem(MeshMBActivityMessage item) {
		updatePeer(item);
		super.addingFutureItem(item);
	}

	@Override
	protected HttpJsonSerialiser<MeshMBActivityMessage, IOException> openPast() throws ServalDInterfaceException, IOException, IOException {
		return serval.getResultClient().meshmbActivity(identity.subscriber);
	}

	@Override
	protected HttpJsonSerialiser<MeshMBActivityMessage, IOException> openFuture() throws ServalDInterfaceException, IOException, IOException {
		return serval.getResultClient().meshmbActivity(identity.subscriber, last == null ? "" : last.token);
	}
}
