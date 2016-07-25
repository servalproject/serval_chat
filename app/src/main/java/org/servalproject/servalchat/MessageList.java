package org.servalproject.servalchat;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.servalproject.mid.ListObserverSet;

/**
 * Created by jeremy on 11/07/16.
 */
public class MessageList
        extends ObservedRecyclerView<MessageList.Msg, MessageList.MessageHolder>
{
    public MessageList(ListObserverSet<Msg> observerSet, Context context, @Nullable AttributeSet attrs) {
        super(observerSet, context, attrs);
    }

    @Override
    protected MessageHolder createHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    protected void bind(MessageHolder holder, Msg item) {

    }

    @Override
    protected Msg get(int position) {
        return null;
    }

    @Override
    protected int getCount() {
        return 0;
    }

    public class Msg{
        // Placeholder
    }

    public class MessageHolder extends RecyclerView.ViewHolder{

        public MessageHolder(View itemView) {
            super(itemView);
        }
    }
}
