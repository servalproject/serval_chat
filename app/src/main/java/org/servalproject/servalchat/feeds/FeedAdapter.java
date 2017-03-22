package org.servalproject.servalchat.feeds;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.MessageFeed;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.views.BasicViewHolder;
import org.servalproject.servalchat.views.ScrollingAdapter;
import org.servalproject.servaldna.meshmb.PlyMessage;

/**
 * Created by jeremy on 8/08/16.
 */
public class FeedAdapter extends ScrollingAdapter<PlyMessage, FeedAdapter.MessageHolder> {
	public FeedAdapter(MessageFeed feed) {
		super(feed);
	}

	@Override
	public MessageHolder create(ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		return new TextHolder(inflater.inflate(R.layout.my_message, parent, false));
	}

	@Override
	protected void bind(MessageHolder holder, PlyMessage item) {
		holder.bind(item);
	}

	public abstract class MessageHolder extends BasicViewHolder {
		public MessageHolder(View itemView) {
			super(itemView);
		}

		public void bind(PlyMessage item) {
		}
	}

	public class TextHolder extends MessageHolder {
		private TextView message;

		public TextHolder(View itemView) {
			super(itemView);
			this.message = (TextView) this.itemView.findViewById(R.id.message);
		}

		public void bind(PlyMessage item) {
			message.setText(item.text);
		}
	}
}
