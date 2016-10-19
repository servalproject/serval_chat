package org.servalproject.mid;

import android.util.Log;

import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.meshms.MeshMSConversation;
import org.servalproject.servaldna.meshms.MeshMSConversationList;
import org.servalproject.servaldna.rhizome.RhizomeListBundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 11/07/16.
 */
public class Messaging {
    private final Serval serval;
    private static final String SERVICE = "MeshMS2";
    private final Identity identity;
    private int hashCode;
    private int unreadCount;
    private static final String TAG = "Messaging";
    public final List<MeshMSConversation> conversations = new ArrayList<>();
    public final ListObserverSet<MeshMSConversation> observers;

    public int getHashCode(){
        return hashCode;
    }
    public int getUnreadCount(){
        return unreadCount;
    }

    Messaging(Serval serval, Identity identity){
        this.serval = serval;
        this.identity = identity;
        this.observers = new ListObserverSet<>(serval.uiHandler);

        // TODO add restful api for conversation list updates?
        serval.rhizome.observerSet.add(rhizomeObserver);
        refresh();
    }

    private final ListObserver<RhizomeListBundle> rhizomeObserver = new ListObserver<RhizomeListBundle>() {
        @Override
        public void added(RhizomeListBundle obj) {
            if (obj.manifest.service.equals(SERVICE)
            && (obj.manifest.recipient.equals(identity.subscriber.sid)
            || obj.manifest.sender.equals(identity.subscriber.sid)))
                refresh();
        }

        @Override
        public void removed(RhizomeListBundle obj) {}

        @Override
        public void updated(RhizomeListBundle obj) {}

        @Override
        public void reset() {}
    };

    private Refresh refreshing;
    void refresh(){
        Log.v(TAG, "Refreshing private messages for "+identity.subscriber.toString());
        synchronized (this) {
            Refresh refreshing = this.refreshing;
            if (refreshing != null)
                refreshing.cancel = true;
            this.refreshing = new Refresh();
        }
        serval.runOnThreadPool(refreshing);
    }

    private class Refresh implements Runnable{
        private boolean cancel = false;

        @Override
        public void run() {
            try {
                // TODO abort on new incoming message?
                Log.v(TAG, "Fetching private messages for "+identity.subscriber.toString());

                List<MeshMSConversation> replace = new ArrayList<>();
                MeshMSConversationList list = serval.getResultClient().meshmsListConversations(identity.subscriber.sid);
                int hashCode = 0;
                int unreadCount = 0;

                try {
                    MeshMSConversation conversation;
                    while ((conversation = list.nextConversation()) != null) {
                        if (cancel) {
                            Log.v(TAG, "Cancelled");
                            return;
                        }
                        if (!conversation.isRead) {
                            hashCode = hashCode ^ conversation.readHashCode();
                            unreadCount++;
                        }
                        replace.add(conversation);
                    }
                }finally{
                    list.close();
                }

                Log.v(TAG, "Found "+unreadCount+" unread of "+ replace.size()+" conversations!");

                synchronized (Messaging.this) {
                    if (cancel)
                        return;
                    conversations.clear();
                    conversations.addAll(replace);
                    Messaging.this.hashCode = hashCode;
                    Messaging.this.unreadCount = unreadCount;
                }
                observers.onReset();
            }catch (Exception e) {
                throw new IllegalStateException(e);
            }finally {
                synchronized (Messaging.this) {
                    if (refreshing == this)
                        refreshing = null;
                }
            }
        }
    }

    public MessageList getPrivateMessages(Subscriber peer) {
        return new MessageList(serval, this, identity.subscriber, peer);
    }
}
