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
    private boolean hasMore = true;
    private final SubscriberId self;
    private final SubscriberId peer;
    private final ListObserverSet<MeshMSMessage> observeFuture;
    private boolean closed = false;
    private MeshMSMessageList futureList;
    private MeshMSMessageList pastList;
    private String token;
    private boolean polling = false;
    private Thread pollingThread;

    MessageList(Serval serval, SubscriberId self, SubscriberId peer){
        this.serval = serval;
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
            Thread p = pollingThread;
            if (p!=null)
                p.interrupt();
        }
    }

    private Runnable readFuture = new Runnable() {
        @Override
        public void run() {
            try {
                pollingThread = Thread.currentThread();
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
                if (!closed)
                    throw new IllegalStateException(e);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                pollingThread = null;
            }

            if (closed){
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
    }

    public boolean moreOldMessages(int maxCount, Collection<MeshMSMessage> messageList) throws ServalDInterfaceException, MeshMSException, IOException {
        if (serval.uiHandler.isUiThread())
            throw new IllegalStateException();
        if (hasMore) {
            if (pastList == null)
                pastList = serval.getResultClient().meshmsListMessages(self, peer);

            for (int i = 0; i < maxCount; i++) {
                MeshMSMessage item = pastList.nextMessage();
                if (item == null) {
                    hasMore = false;
                    break;
                }
                if (token == null) {
                    token = item.token;
                    start();
                }
                messageList.add(item);
            }
/*
            if (token == null){
                // TODO fix newsince API to allow waiting for new messages when the pastList is empty
                token = "";
                serval.runOnThreadPool(readFuture);
            }
            */
        }
        if (!hasMore && pastList !=null) {
            pastList.close();
            pastList = null;
        }
        return hasMore;
    }

    public void close() {
        hasMore = false;
        polling = false;
        closed = true;
        Thread p = pollingThread;
        if (p!=null)
            p.interrupt();
    }
}
