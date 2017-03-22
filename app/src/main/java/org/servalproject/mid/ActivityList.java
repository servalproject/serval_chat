package org.servalproject.mid;

import org.servalproject.servaldna.AbstractJsonList;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.meshmb.MeshMBActivityMessage;
import org.servalproject.servaldna.meshmb.MeshMBSubscription;

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

	@Override
	protected AbstractJsonList<MeshMBActivityMessage, IOException> openPast() throws ServalDInterfaceException, IOException, IOException {
		return serval.getResultClient().meshmbActivity(identity.subscriber);
	}

	@Override
	protected AbstractJsonList<MeshMBActivityMessage, IOException> openFuture() throws ServalDInterfaceException, IOException, IOException {
		return serval.getResultClient().meshmbActivity(identity.subscriber, last == null ? "" : last.token);
	}
}
