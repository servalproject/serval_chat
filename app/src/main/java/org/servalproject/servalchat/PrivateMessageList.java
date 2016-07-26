package org.servalproject.servalchat;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.ListObserver;
import org.servalproject.mid.MessageList;
import org.servalproject.servaldna.AbstractId;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.SubscriberId;
import org.servalproject.servaldna.meshms.MeshMSMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 11/07/16.
 */
public class PrivateMessageList
        extends InfiniteRecyclerView<MeshMSMessage, PrivateMessageList.MessageHolder>
        implements ILifecycle, INavigate, ListObserver<MeshMSMessage> {

    private MainActivity activity;
    private MessageList list;
    private static final String TAG = "PrivateMessages";

    public PrivateMessageList(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setHasFixedSize(true);
        listAdapter.setHasStableIds(true);
    }

    @Override
    protected void fetchMore() {
        AsyncTask<Void, Void, Void> fetch = new AsyncTask<Void, Void, Void>() {
            private List<MeshMSMessage> messages;
            private Exception e;
            private boolean hasMore;

            @Override
            protected void onPostExecute(Void aVoid) {
                if (e!=null) {
                    activity.showError(e);
                    e = null;
                } else {
                    Log.v(TAG, "Adding "+messages.size()+" ("+hasMore+")");
                    addPast(messages);
                    messages = null;
                    fetchComplete(!hasMore);
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    List<MeshMSMessage> messages = new ArrayList<>();
                    Log.v(TAG, "Fetching more... ");
                    hasMore = list.moreOldMessages(10, messages);
                    this.messages = messages;
                    this.e = null;
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    this.messages = null;
                    this.e = e;
                }
                return null;
            }
        };
        fetch.execute((Void[]) null);
    }

    @Override
    protected int getItemType(MeshMSMessage item) {
        return item.type.ordinal();
    }

    @Override
    protected long getId(MeshMSMessage item) {
        return item._rowNumber;
    }

    @Override
    protected MessageHolder createHolder(ViewGroup parent, int viewType) {
        MeshMSMessage.Type type = MeshMSMessage.Type.values()[viewType];
        Log.v(TAG, "Creating holder for "+type);
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (type){
            case MESSAGE_SENT: {
                View view = inflater.inflate(R.layout.my_message, parent, false);
                return new OurMessage(view);
            }
            case MESSAGE_RECEIVED: {
                View view = inflater.inflate(R.layout.their_message, parent, false);
                return new TheirMessage(view);
            }
            case ACK_RECEIVED:{
                View view = inflater.inflate(R.layout.message_ack, parent, false);
                return new Ack(view);
            }
        }
        return null;
    }

    @Override
    protected void bind(MessageHolder holder, MeshMSMessage item) {
        holder.bind(item);
    }

    @Override
    public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args){
        try {
            this.activity = activity;
            // TODO peer details?
            SubscriberId sid = new SubscriberId(args.getByteArray("them"));
            Subscriber peer = new Subscriber(sid);
            list = id.messaging.getPrivateMessages(peer);
            return this;
        } catch (AbstractId.InvalidBinaryException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onDetach() {
        list.close();
    }

    // TODO implement pause / resume on MessageList
    @Override
    public void onVisible() {
        list.observe(this);
        begin();
    }

    @Override
    public void onHidden() {
        list.stopObserving(this);
    }

    @Override
    public void added(MeshMSMessage obj) {
        addFuture(obj);
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

    public abstract class MessageHolder extends RecyclerView.ViewHolder{
        public MessageHolder(View itemView) {
            super(itemView);
        }

        abstract void bind(MeshMSMessage message);
    }

    public class OurMessage extends MessageHolder{
        TextView message;
        public OurMessage(View itemView) {
            super(itemView);
            message = (TextView)itemView.findViewById(R.id.message);
        }

        @Override
        void bind(MeshMSMessage message) {
            this.message.setText(message.text);
        }
    }

    public class TheirMessage extends MessageHolder{
        TextView message;
        public TheirMessage(View itemView) {
            super(itemView);
            message = (TextView)itemView.findViewById(R.id.message);
        }

        @Override
        void bind(MeshMSMessage message) {
            this.message.setText(message.text);
        }
    }

    public class Ack extends MessageHolder{
        public Ack(View itemView) {
            super(itemView);
        }

        @Override
        void bind(MeshMSMessage message) {

        }
    }

}
