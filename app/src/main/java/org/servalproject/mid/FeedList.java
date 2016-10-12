package org.servalproject.mid;

import android.util.Log;

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
        Log.v(TAG, "Connecting past list...");
        list.connect();
        return list;
    }

    @Override
    protected AbstractJsonList<RhizomeListBundle, IOException> openFuture() throws ServalDInterfaceException, IOException {
        RhizomeBundleList list = new RhizomeBundleList(serval.getResultClient(), token);
        list.setServiceFilter(MeshMBCommon.SERVICE);
        Log.v(TAG, "Connecting future list...");
        list.connect();
        return list;
    }

    @Override
    protected void addingFutureItem(RhizomeListBundle item) {
        Log.v(TAG, "Adding future "+item.manifest.id.abbreviation()+" - "+item.manifest.name);
        token = item.token;
        // TODO verify that the sender and id are for the same identity!
        // for now we can assume this, but we might break this rule in a future version
        Subscriber subscriber = new Subscriber(item.manifest.sender, item.manifest.id, true);
        Peer p = serval.knownPeers.getPeer(subscriber);
        p.updateFeedName(item.manifest.name);
        super.addingFutureItem(item);
    }

    @Override
    protected void addingPastItem(RhizomeListBundle item) {
        if (item==null){
            Log.v(TAG, "End of past items");
        } else {
            Log.v(TAG, "Adding past " + item.manifest.id.abbreviation() + " - " + item.manifest.name);
        }

        if (token == null) {
            token = (item == null) ? "" : item.token;
            start();
        }
        super.addingPastItem(item);
    }
}
