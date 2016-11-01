package org.servalproject.mid;

import org.servalproject.servaldna.AbstractJsonList;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.meshmb.MeshMBCommon;
import org.servalproject.servaldna.rhizome.RhizomeBundleList;
import org.servalproject.servaldna.rhizome.RhizomeListBundle;

import java.io.IOException;

/**
 * Created by jeremy on 11/10/16.
 */
public class FeedList extends AbstractGrowingList<RhizomeListBundle, IOException> {
	private RhizomeListBundle last;
	private String token;
	private static final String TAG = "FeedList";

	public FeedList(Serval serval) {
		super(serval);
	}

	@Override
	protected void start() {
		if (token == null)
			return;
		super.start();
	}

	@Override
	protected AbstractJsonList<RhizomeListBundle, IOException> openPast() throws ServalDInterfaceException, IOException {
		RhizomeBundleList list = new RhizomeBundleList(serval.getResultClient());
		list.setServiceFilter(MeshMBCommon.SERVICE);
		list.connect();
		return list;
	}

	@Override
	protected AbstractJsonList<RhizomeListBundle, IOException> openFuture() throws ServalDInterfaceException, IOException {
		RhizomeBundleList list = new RhizomeBundleList(serval.getResultClient(), token);
		list.setServiceFilter(MeshMBCommon.SERVICE);
		list.connect();
		return list;
	}

	private void updatePeer(RhizomeListBundle item) {
		// TODO verify that the sender and id are for the same identity!
		// for now we can assume this, but we might break this rule in a future version
		Subscriber subscriber = new Subscriber(
				item.author != null ? item.author : item.manifest.sender,
				item.manifest.id, true);
		Peer p = serval.knownPeers.getPeer(subscriber);
		p.updateFeedName(item.manifest.name);
	}

	@Override
	protected void addingFutureItem(RhizomeListBundle item) {
		if (last == null || last.compareTo(item)<0){
			last = item;
			token = item.token;
		}
		updatePeer(item);
		super.addingFutureItem(item);
	}

	@Override
	protected void addingPastItem(RhizomeListBundle item) {
		if (item != null)
			updatePeer(item);

		if (token == null) {
			last = item;
			token = (item == null) ? "" : item.token;
			start();
		}
		super.addingPastItem(item);
	}
}
