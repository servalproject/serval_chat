package org.servalproject.servalchat.views;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.MessageFeed;
import org.servalproject.servalchat.R;

/**
 * Created by jeremy on 8/08/16.
 */
public class FeedAdapter extends ScrollingAdapter<MessageFeed.Message, FeedAdapter.MessageHolder>{
    public FeedAdapter(MessageFeed feed){
        super(feed);
    }

    private static final int SPINNER = 0;
    private static final int MESSAGE = 1;
    @Override
    public MessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType){
            case 0:
                return new SpinnerHolder(inflater.inflate(R.layout.progress, parent, false));
            default:
                return new TextHolder(inflater.inflate(R.layout.my_message, parent, false));
        }
    }

    @Override
    protected void bind(MessageHolder holder, MessageFeed.Message item) {
        holder.bind(item);
    }

    @Override
    protected int getItemType(MessageFeed.Message item) {
        return (item == null) ? SPINNER : MESSAGE;
    }

    public abstract class MessageHolder extends RecyclerView.ViewHolder{
        public MessageHolder(View itemView) {
            super(itemView);
        }
        public void bind(MessageFeed.Message item){}
    }

    public class SpinnerHolder extends MessageHolder{
        public SpinnerHolder(View itemView) {
            super(itemView);
        }
    }

    public class TextHolder extends MessageHolder{
        private TextView message;
        public TextHolder(View itemView) {
            super(itemView);
            this.message = (TextView)this.itemView.findViewById(R.id.message);
        }
        public void bind(MessageFeed.Message item){
            message.setText(item.text);
        }
    }
}
