package org.servalproject.servalchat.feeds;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
	}

	@Override
	protected void bindItem(MessageHolder holder, int position) {
		MeshMBActivityMessage prev = (position>0) ? getItem(position -1) : null;
		MeshMBActivityMessage item = getItem(position);
		MeshMBActivityMessage next = (position+1<getItemCount()) ? getItem(position +1) : null;
		holder.bind(prev, item, next);
	}

	@Override
	public void insertedItem(MeshMBActivityMessage item, int position) {
		super.insertedItem(item, position);
		if (position>0)
			notifyItemChanged(position -1);
		if (position+1<getItemCount())
			notifyItemChanged(position +1);
	}

	@Override
	protected void addItem(int index, MeshMBActivityMessage item) {
		super.addItem(index, item);
	}

	@Override
	public MessageHolder create(ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		return new MessageHolder(inflater.inflate(R.layout.feed_message, parent, false));
	}

	private int padding;
	@Override
	public void onAttachedToRecyclerView(RecyclerView recyclerView) {
		padding = recyclerView.getResources().getDimensionPixelSize(R.dimen.item_padding);
		super.onAttachedToRecyclerView(recyclerView);
	}

	public class MessageHolder extends BasicViewHolder implements View.OnClickListener {
		private TextView message;
		private TextView name;
		private Subscriber subscriber;
		private long offset;

		public MessageHolder(View itemView) {
			super(itemView);
			this.message = (TextView) this.itemView.findViewById(R.id.message);
			this.name = (TextView) this.itemView.findViewById(R.id.name);
			this.itemView.setOnClickListener(this);
		}

		public void bind(MeshMBActivityMessage prev, MeshMBActivityMessage item, MeshMBActivityMessage next) {
			this.subscriber = item.subscriber;
			this.offset = item.offset;
			message.setText(item.text);
			name.setText(item.name);
			boolean prevDifferent = prev==null || !prev.subscriber.equals(item.subscriber);
			name.setVisibility( prevDifferent ? View.VISIBLE : View.GONE);
			RecyclerView.LayoutParams parms = (RecyclerView.LayoutParams) itemView.getLayoutParams();
			parms.topMargin = prevDifferent ? padding : 0;
			parms.bottomMargin = (next == null || !next.subscriber.equals(item.subscriber) ? padding : 0);
		}

		@Override
		public void onClick(View v) {
			presenter.openFeed(subscriber, offset);
		}
	}
}
