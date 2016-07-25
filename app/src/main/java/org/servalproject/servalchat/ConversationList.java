package org.servalproject.servalchat;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Messaging;
import org.servalproject.servaldna.meshms.MeshMSConversation;

/**
 * Created by jeremy on 13/07/16.
 */
public class ConversationList
        extends ObservedRecyclerView<MeshMSConversation, ConversationList.ConversationHolder>
        implements INavigate{

    private Messaging messaging;
    public ConversationList(Context context, @Nullable AttributeSet attrs) {
        super(null, context, attrs);
    }

    @Override
    protected ConversationHolder createHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    protected void bind(ConversationHolder holder, MeshMSConversation item) {

    }

    @Override
    protected MeshMSConversation get(int position) {
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
        return this;
    }

    public class ConversationHolder extends RecyclerView.ViewHolder {

        public ConversationHolder(View itemView) {
            super(itemView);
        }
    }
}
