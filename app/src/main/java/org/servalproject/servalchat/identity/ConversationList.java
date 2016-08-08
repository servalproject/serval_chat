package org.servalproject.servalchat.identity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.KnownPeers;
import org.servalproject.mid.Messaging;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.ObservedRecyclerView;
import org.servalproject.servalchat.R;
import org.servalproject.servaldna.meshms.MeshMSConversation;

/**
 * Created by jeremy on 13/07/16.
 */
public class ConversationList
        extends ObservedRecyclerView<MeshMSConversation, ConversationList.ConversationHolder>
        implements INavigate {

    private Messaging messaging;
    private static final String TAG = "ConversationList";

    public ConversationList(Context context, @Nullable AttributeSet attrs) {
        super(null, context, attrs);
        setHasFixedSize(true);
        setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    protected ConversationHolder createHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_item, parent, false);
        return new ConversationHolder(view);
    }

    @Override
    protected void bind(ConversationHolder holder, MeshMSConversation item) {
        holder.name.setText(item.them.sid.abbreviation());
        holder.conversation = item;
    }

    @Override
    protected MeshMSConversation get(int position) {
        if (messaging == null)
            return null;
        return messaging.conversations.get(position);
    }

    @Override
    protected int getCount() {
        if (messaging == null)
            return 0;
        return messaging.conversations.size();
    }

    @Override
    public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args){
        this.messaging = id.messaging;
        this.setObserverSet(messaging.observers);
        return super.onAttach(activity, n, id, args);
    }

    public class ConversationHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView name;
        MeshMSConversation conversation;

        public ConversationHolder(View itemView) {
            super(itemView);
            name = (TextView)this.itemView.findViewById(R.id.name);
            name.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Bundle args = new Bundle();
            KnownPeers.saveSubscriber(conversation.them, args);
            activity.go(identity, Navigation.PrivateMessages, args);
        }
    }
}
