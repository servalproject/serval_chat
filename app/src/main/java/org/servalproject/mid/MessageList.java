package org.servalproject.mid;

import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.meshms.MeshMSException;
import org.servalproject.servaldna.meshms.MeshMSMessage;
import org.servalproject.servaldna.meshms.MeshMSMessageList;

import java.io.IOException;

/**
 * Created by jeremy on 11/07/16.
 */
public class MessageList implements IObservableList<MeshMSMessage, MeshMSException>{
    private final Serval serval;
    private final Messaging messaging;
    private boolean hasMore = true;
    public final Subscriber self;
    public final Subscriber peer;
    private final ListObserverSet<MeshMSMessage> observeFuture;
    private boolean closed = false;
    private MeshMSMessageList futureList;
    private MeshMSMessageList pastList;
    private String token;
    private boolean polling = false;

    MessageList(Serval serval, Messaging messaging, Subscriber self, Subscriber peer){
        this.serval = serval;
        this.messaging = messaging;
        this.self = self;
        this.peer = peer;
        this.observeFuture = new ListObserverSet<>(serval.uiHandler);
    }

    private void start(){
        if (polling || token== null || !observeFuture.hasObservers())
            return;
        polling = true;
        serval.runOnThreadPool(readFuture);
    }

    public void observe(ListObserver<MeshMSMessage> observer){
        observeFuture.add(observer);
        start();
    }

    public void stopObserving(ListObserver<MeshMSMessage> observer){
        observeFuture.remove(observer);
        if (!observeFuture.hasObservers()){
            polling = false;
            if (futureList != null){
                try {
                    futureList.close();
                } catch (IOException e) {}
                futureList = null;
            }
        }
    }

    private Runnable readFuture = new Runnable() {
        @Override
        public void run() {
            try {
                while (polling) {
                    MeshMSMessageList list = futureList = serval.getResultClient().meshmsListMessagesSince(self.sid, peer.sid, token);
                    MeshMSMessage item;
                    while (polling && (item = list.nextMessage()) != null) {
                        token = item.token;
                        observeFuture.onAdd(item);
                    }
                    // on graceful close, restart
                    list.close();
                    futureList = null;
                }
            } catch (IOException e) {
                // ignore if we caused this deliberately in another thread.
                if (polling)
                    throw new IllegalStateException(e);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    };

    public void sendMessage(String message) throws ServalDInterfaceException, MeshMSException, IOException {
        if (serval.uiHandler.isUiThread())
            throw new IllegalStateException();
        serval.getResultClient().meshmsSendMessage(self.sid, peer.sid, message);
    }

    public void markRead() throws ServalDInterfaceException, MeshMSException, IOException {
        if (serval.uiHandler.isUiThread())
            throw new IllegalStateException();
        serval.getResultClient().meshmsMarkAllMessagesRead(self.sid, peer.sid);
        messaging.refresh();
    }

    public MeshMSMessage next() throws ServalDInterfaceException, MeshMSException, IOException {
        if (!hasMore)
            return null;
        if (pastList == null)
            pastList = serval.getResultClient().meshmsListMessages(self.sid, peer.sid);

        MeshMSMessage item = pastList.nextMessage();
        if (token == null) {
            token = (item == null) ? "" : item.token;
            start();
        }

        if (item == null) {
            hasMore = false;
            pastList.close();
            pastList = null;
            return null;
        }
        return item;
    }

    public void close() {
        hasMore = false;
        polling = false;
        closed = true;
        if (pastList !=null) {
            try {
                pastList.close();
            } catch (IOException e) {}
            pastList = null;
        }
        if (futureList != null){
            try {
                futureList.close();
            } catch (IOException e) {}
            futureList = null;
        }
    }
}
