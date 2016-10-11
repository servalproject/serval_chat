package org.servalproject.servalchat.peer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.KnownPeers;
import org.servalproject.mid.MessageList;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.views.Presenter;
import org.servalproject.servalchat.views.PresenterFactory;
import org.servalproject.servalchat.views.ScrollingAdapter;
import org.servalproject.servaldna.AbstractId;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.meshms.MeshMSMessage;

/**
 * Created by jeremy on 27/07/16.
 */
public final class PrivateMessagingPresenter extends Presenter<PrivateMessaging>{
    private MessageList messages;

    private static final String TAG = "PrivateMessaging";
    private boolean sending = false;
    private ScrollingAdapter<MeshMSMessage, ItemHolder> adapter;

    private PrivateMessagingPresenter(PresenterFactory<PrivateMessaging, ?> factory, String key, Identity identity) {
        super(factory, key, identity);
    }

    public static PresenterFactory<PrivateMessaging, PrivateMessagingPresenter> factory
            = new PresenterFactory<PrivateMessaging, PrivateMessagingPresenter>() {

        @Override
        protected String getKey(Identity id, Bundle savedState) {
            try {
                Subscriber them = KnownPeers.getSubscriber(savedState);
                return id.subscriber.sid.toHex()+":"+them.sid.toHex();
            } catch (AbstractId.InvalidBinaryException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        protected PrivateMessagingPresenter create(String key, Identity id) {
            return new PrivateMessagingPresenter(this, key, id);
        }

    };

    @Override
    protected void bind() {
        PrivateMessaging view = getView();
        view.message.setEnabled(!sending);
        view.send.setEnabled(!sending);
        view.list.setAdapter(adapter);
    }

    @Override
    protected void save(Bundle config) {
        super.save(config);
        KnownPeers.saveSubscriber(messages.peer, config);
    }

    @Override
    protected void restore(Bundle config) {
        try {
            // TODO peer details?
            Subscriber peer = KnownPeers.getSubscriber(config);
            messages = identity.messaging.getPrivateMessages(peer);
            adapter = new ScrollingAdapter<MeshMSMessage, ItemHolder>(messages){
                @Override
                public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                    if (viewType == 4)
                        return new SpinnerHolder(inflater, parent);
                    boolean mine = MeshMSMessage.Type.values()[viewType] == MeshMSMessage.Type.MESSAGE_SENT;
                    return new MessageHolder(inflater, parent, mine);
                }

                @Override
                protected void addItem(MeshMSMessage item, boolean inPast) {
                    if (item.type == MeshMSMessage.Type.ACK_RECEIVED) {
                        // TODO display "delivered" marker
                        return;
                    }
                    // TODO sort future messages that arrive in a burst!
                    super.addItem(item, inPast);
                }

                @Override
                protected void bind(ItemHolder holder, MeshMSMessage item) {
                    holder.bind(item);
                }

                @Override
                protected int getItemType(MeshMSMessage item) {
                    if (item == null)
                        return 4;
                    return item.type.ordinal();
                }

                @Override
                public long getItemId(int position) {
                    MeshMSMessage item = getItem(position);
                    if (item == null)
                        return Long.MAX_VALUE;
                    return item.getId();
                }
            };
            adapter.setHasStableIds(true);
        } catch (AbstractId.InvalidBinaryException e) {
            throw new IllegalStateException(e);
        }
    }

    void onDestroy(){
        adapter.clear();
        Serval.getInstance().runOnThreadPool(new Runnable() {
            @Override
            public void run() {
                try {
                    messages.markRead();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    @Override
    public void onVisible() {
        super.onVisible();
        adapter.onVisible();
    }

    @Override
    public void onHidden() {
        super.onHidden();
        adapter.onHidden();
    }

    public void send(final String message){
        AsyncTask<Void, Void, Void> sender = new AsyncTask<Void, Void, Void>() {
            private Exception e;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                sending = true;
                PrivateMessaging view = getView();
                view.message.setEnabled(false);
                view.send.setEnabled(false);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                PrivateMessaging view = getView();
                if (view == null)
                    return;
                if (e != null){
                    view.activity.showError(e);
                }else{
                    view.message.setText("");
                }
                view.message.setEnabled(true);
                view.send.setEnabled(true);
                sending = false;
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    messages.sendMessage(message);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    this.e = e;
                }
                return null;
            }
        };
        sender.execute();
    }


    public abstract class ItemHolder extends RecyclerView.ViewHolder{
        public ItemHolder(LayoutInflater inflater, ViewGroup parent, int layoutResource) {
            super(inflater.inflate(layoutResource, parent, false));
        }
        public void bind(MeshMSMessage item){}
    }

    public class MessageHolder extends ItemHolder{
        TextView message;

        public MessageHolder(LayoutInflater inflater, ViewGroup parent, boolean myMessage) {
            super(inflater, parent, myMessage ? R.layout.my_message : R.layout.their_message);
            this.message = (TextView)this.itemView.findViewById(R.id.message);
        }

        @Override
        public void bind(MeshMSMessage item){
            message.setText(item.text);
        }
    }

    public class SpinnerHolder extends ItemHolder{
        public SpinnerHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater, parent, R.layout.progress);
        }
    }
}
