package org.servalproject.servalchat.peer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.ListObserver;
import org.servalproject.mid.MessageList;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.views.Presenter;
import org.servalproject.servalchat.views.PresenterFactory;
import org.servalproject.servalchat.R;
import org.servalproject.servaldna.AbstractId;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.SubscriberId;
import org.servalproject.servaldna.meshms.MeshMSMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 27/07/16.
 */
public final class PrivateMessagingPresenter extends Presenter<PrivateMessaging>
        implements ListObserver<MeshMSMessage> {
    private MessageList messages;

    private List<MeshMSMessage> future;
    private List<MeshMSMessage> past;
    private static final String TAG = "PrivateMessaging";
    private boolean sending = false;
    private boolean fetching = false;
    private RecyclerView.Adapter<ItemHolder> adapter;
    private boolean hasMore = true;

    private PrivateMessagingPresenter(PresenterFactory<PrivateMessaging, ?> factory, String key, Identity identity) {
        super(factory, key, identity);
    }

    public static PresenterFactory<PrivateMessaging, PrivateMessagingPresenter> factory
            = new PresenterFactory<PrivateMessaging, PrivateMessagingPresenter>() {

        @Override
        protected String getKey(Identity id, Bundle savedState) {
            try {
                SubscriberId them = new SubscriberId(savedState.getByteArray("them"));
                return id.subscriber.sid.toHex()+":"+them.toHex();
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
        view.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                testPosition();
            }
        });
    }

    private void testPosition(){
        if (fetching || !hasMore)
            return;

        PrivateMessaging view = getView();
        int lastVisible = view.layoutManager.findLastVisibleItemPosition();
        final int fetchCount = lastVisible + 15 - (past.size() + future.size());

        if (fetchCount<=0)
            return;

        Log.v(TAG, "Fetching "+fetchCount+" more items ("+lastVisible+", "+past.size()+", "+future.size()+")");
        fetching = true;

        final AsyncTask<Void, MeshMSMessage, Void> fetch = new AsyncTask<Void, MeshMSMessage, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try{
                    for (int i=0;i<fetchCount;i++){
                        MeshMSMessage msg = messages.nextMessage();
                        publishProgress(msg);
                        if (msg == null)
                            break;
                    }
                }catch (Exception e){
                    throw new IllegalStateException(e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                fetching = false;
                testPosition();
            }

            @Override
            protected void onProgressUpdate(MeshMSMessage... values) {
                super.onProgressUpdate(values);
                MeshMSMessage msg = values[0];
                if (msg == null) {
                    hasMore = false;
                    adapter.notifyItemRemoved(past.size() + future.size());
                }else
                    addItem(msg, true);
            }
        };
        fetch.execute();
    }

    @Override
    protected void save(Bundle config) {
        super.save(config);
        config.putByteArray("them", messages.peer.getBinary());
    }

    @Override
    protected void restore(Bundle config) {
        try {
            // TODO peer details?
            SubscriberId sid = new SubscriberId(config.getByteArray("them"));
            Subscriber peer = new Subscriber(sid);
            messages = identity.messaging.getPrivateMessages(peer);
            future = new ArrayList<>();
            past = new ArrayList<>();
            adapter = new RecyclerView.Adapter<ItemHolder>() {
                @Override
                public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                    if (viewType == 4)
                        return new SpinnerHolder(inflater, parent);
                    boolean mine = MeshMSMessage.Type.values()[viewType] == MeshMSMessage.Type.MESSAGE_SENT;
                    return new MessageHolder(inflater, parent, mine);
                }

                private MeshMSMessage getItem(int position){
                    if (position<0)
                        return null;
                    int futureSize = future.size();
                    if (position < futureSize)
                        return future.get(futureSize - 1 - position);
                    position -= futureSize;
                    if (position<past.size())
                        return past.get(position);
                    return null;
                }

                @Override
                public void onBindViewHolder(ItemHolder holder, int position) {
                    holder.bind(getItem(position));
                }

                @Override
                public int getItemViewType(int position) {
                    MeshMSMessage item = getItem(position);
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

                @Override
                public int getItemCount() {
                    int count = past.size() + future.size();
                    if (hasMore)
                        count++;
                    return count;
                }
            };
            adapter.setHasStableIds(true);
        } catch (AbstractId.InvalidBinaryException e) {
            throw new IllegalStateException(e);
        }
    }

    void onDestroy(){
        messages.close();
        future.clear();
        past.clear();
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
        messages.observe(this);
        testPosition();
    }

    @Override
    public void onHidden() {
        super.onHidden();
        messages.stopObserving(this);
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

    private void addItem(MeshMSMessage item, boolean inPast){
        if (item.type == MeshMSMessage.Type.ACK_RECEIVED){
            // TODO display "delivered" marker
            return;
        }

        if (inPast){
            past.add(item);
            adapter.notifyItemInserted(past.size() + future.size() - 1);
        }else{
            future.add(item);
            adapter.notifyItemInserted(0);
            PrivateMessaging view = getView();
            if (view != null)
                view.layoutManager.scrollToPosition(0);
        }
    }

    @Override
    public void added(MeshMSMessage obj) {
        addItem(obj, false);
    }

    @Override
    public void removed(MeshMSMessage obj) {

    }

    @Override
    public void updated(MeshMSMessage obj) {

    }

    @Override
    public void reset() {

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
