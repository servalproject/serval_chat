package org.servalproject.mid;

import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.SubscriberId;
import org.servalproject.servaldna.meshms.MeshMSException;
import org.servalproject.servaldna.meshms.MeshMSMessage;
import org.servalproject.servaldna.meshms.MeshMSMessageList;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by jeremy on 11/07/16.
 */
public class MessageList {
    private final Serval serval;
    private final Messaging messaging;
    private boolean hasMore = true;
    public final SubscriberId self;
    public final SubscriberId peer;
    private final ListObserverSet<MeshMSMessage> observeFuture;
    private boolean closed = false;
    private MeshMSMessageList futureList;
    private MeshMSMessageList pastList;
    private String token;
    private boolean polling = false;

    MessageList(Serval serval, Messaging messaging, SubscriberId self, SubscriberId peer){
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
                    MeshMSMessageList list = futureList = serval.getResultClient().meshmsListMessagesSince(self, peer, token);
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
        serval.getResultClient().meshmsSendMessage(self, peer, message);
    }

    public void markRead() throws ServalDInterfaceException, MeshMSException, IOException {
        if (serval.uiHandler.isUiThread())
            throw new IllegalStateException();
        serval.getResultClient().meshmsMarkAllMessagesRead(self, peer);
        messaging.refresh();
    }

    public boolean hasMore(){
        return hasMore;
    }

    public MeshMSMessage nextMessage() throws ServalDInterfaceException, MeshMSException, IOException {
        if (!hasMore)
            return null;
        if (pastList == null)
            pastList = serval.getResultClient().meshmsListMessages(self, peer);

        MeshMSMessage item = pastList.nextMessage();
        if (item == null) {
            hasMore = false;
            pastList.close();
            pastList = null;
            return null;
        }
        if (token == null) {
            token = item.token;
            start();
        }
        return item;
    }

    public boolean moreOldMessages(int maxCount, Collection<MeshMSMessage> messageList) throws ServalDInterfaceException, MeshMSException, IOException {
        for (int i = 0; i < maxCount; i++) {
            MeshMSMessage item = nextMessage();
            if (item==null)
                return false;
            messageList.add(item);
        }
        return true;
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
