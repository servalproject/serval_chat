package org.servalproject.mid;

import org.servalproject.servaldna.AbstractJsonList;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.SigningKey;
import org.servalproject.servaldna.meshmb.PlyMessage;

import java.io.IOException;

/**
 * Created by jeremy on 3/08/16.
 */
public class MessageFeed extends AbstractGrowingList<PlyMessage, IOException>{
    private final SigningKey id;
    private String token;

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
        return serval.getResultClient().meshmbListMessages(id);
    }

    @Override
    protected AbstractJsonList<PlyMessage, IOException> openFuture() throws ServalDInterfaceException, IOException {
        return serval.getResultClient().meshmbListMessagesSince(id, token);
    }

    @Override
    protected void addingFutureItem(PlyMessage item) {
        token = item.token;
        super.addingFutureItem(item);
    }

    @Override
    protected void addingPastItem(PlyMessage item) {
        if (token == null) {
            token = (item == null) ? "" : item.token;
            start();
        }
        super.addingPastItem(item);
    }

}
