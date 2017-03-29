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
import org.servalproject.servalchat.views.TimestampView;
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
		return new MessageHolder(inflater.inflate(R.layout.feed_message, parent, false));
	}

	@Override
	protected void bind(MessageHolder holder, PlyMessage item) {
	}

	@Override
	protected void bindItem(MessageHolder holder, int position) {
		holder.bind(getItem(position), position>0?getItem(position - 1):null);
	}

	@Override
	public void insertedItem(PlyMessage item, int position) {
		super.insertedItem(item, position);
		if (position+1<getItemCount())
			notifyItemChanged(position +1);
	}

	public class MessageHolder extends BasicViewHolder {
		private TextView message;
		private TimestampView age;
		public MessageHolder(View itemView) {
			super(itemView);
			this.message = (TextView) this.itemView.findViewById(R.id.message);
			this.age = (TimestampView) this.itemView.findViewById(R.id.age);
		}

		public void bind(PlyMessage item, PlyMessage prev) {
			message.setText(item.text);
			age.setDates(item.date, prev==null?null:prev.date);
		}
	}
}
