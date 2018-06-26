package org.servalproject.servalchat;

import org.servalproject.mid.Identity;
import org.servalproject.mid.IdentityFeed;
import org.servalproject.mid.ListObserver;
import org.servalproject.mid.MessageList;
import org.servalproject.mid.Observer;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.mid.Server;
import org.servalproject.mid.networking.AbstractListObserver;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.meshmb.MeshMBCommon;
import org.servalproject.servaldna.meshms.MeshMSException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 1/05/17.
 */

public class SampleData {
	private static SampleData instance;

	private final Serval serval;
	public SampleData(final Serval serval) {
		this.serval = serval;
		serval.identities.listObservers.addBackground(new AbstractListObserver<Identity>() {
			@Override
			public void reset() {
				createTestData(serval);
			}
		});
	}

	public static void createTestData(Serval serval){
		try {
			if (!serval.identities.isLoaded()
					|| serval.identities.getIdentities().size()>0)
				return;

			List<Identity> identities = new ArrayList<>();
			List<Peer> peers = new ArrayList<>();
			for (int i=0;i<4;i++) {
				Identity id = serval.identities.addIdentity("", "Sample User " + i, "");
				peers.add(serval.knownPeers.getPeer(id.subscriber));
				identities.add(id);
			}
			for (int i=0;i<identities.size();i++) {
				Identity id = identities.get(i);
				IdentityFeed feed = id.getFeed();
				for (int j = 0; j < 5; j++)
					feed.sendMessage("Message " + j);
			}
			for (int i=0;i<identities.size();i++){
				Identity id = identities.get(i);
				for (int j=0;j<identities.size();j++){
					if (i==j)
						continue;

					Peer p = peers.get(j);
					MessageList messages = id.messaging.getPrivateMessages(p.getSubscriber());
					messages.sendMessage("Hi "+p.displayName()+" from "+id.getName());

					if ((i+j)%4==2)
						continue;

					id.alterSubscription(MeshMBCommon.SubscriptionAction.Follow, p);
				}
			}

		}catch (ServalDInterfaceException |
				IOException e){
			throw new IllegalStateException(e);
		}
	}

	public static void init(Serval serval){
		if (instance!=null)
			throw new IllegalStateException();
		instance = new SampleData(serval);
	}
}
