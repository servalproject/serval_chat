package org.servalproject.servalchat.feeds;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.IObservableList;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.views.BasicViewHolder;
import org.servalproject.servalchat.views.ScrollingAdapter;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.meshmb.MeshMBActivityMessage;

/**
 * Created by jeremy on 30/01/17.
 */

public class ActivityAdapter extends ScrollingAdapter<MeshMBActivityMessage, ActivityAdapter.MessageHolder> {
	private final ActivityPresenter presenter;

	public ActivityAdapter(IObservableList<MeshMBActivityMessage, ?> list, ActivityPresenter presenter) {
		super(list);
		this.presenter = presenter;
	}

	@Override
	protected void bind(MessageHolder holder, MeshMBActivityMessage item) {
		holder.bind(item);
	}

	@Override
	public MessageHolder create(ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		return new MessageHolder(inflater.inflate(R.layout.feed_message, parent, false));
	}

	public class MessageHolder extends BasicViewHolder implements View.OnClickListener {
		private TextView message;
		private Subscriber subscriber;
		private long offset;

		public MessageHolder(View itemView) {
			super(itemView);
			this.message = (TextView) this.itemView.findViewById(R.id.message);
			this.itemView.setOnClickListener(this);
		}

		public void bind(MeshMBActivityMessage item) {
			this.subscriber = item.subscriber;
			this.offset = item.offset;
			message.setText(item.text);
		}

		@Override
		public void onClick(View v) {
			presenter.openFeed(subscriber, offset);
		}
	}
}
