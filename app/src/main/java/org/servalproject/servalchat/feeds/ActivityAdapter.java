package org.servalproject.servalchat.feeds;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.servalproject.mid.IObservableList;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.views.BasicViewHolder;
import org.servalproject.servalchat.views.Identicon;
import org.servalproject.servalchat.views.ScrollingAdapter;
import org.servalproject.servalchat.views.TimestampView;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.meshmb.MeshMBActivityMessage;

/**
 * Created by jeremy on 30/01/17.
 */

public class ActivityAdapter extends ScrollingAdapter<MeshMBActivityMessage, ActivityAdapter.MessageHolder> {
	private final MyFeedPresenter presenter;

	public ActivityAdapter(IObservableList<MeshMBActivityMessage, ?> list, MyFeedPresenter presenter) {
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
		holder.bind(prev, item);
	}

	@Override
	public void insertedItem(MeshMBActivityMessage item, int position) {
		super.insertedItem(item, position);
		if (position+1<getItemCount())
			notifyItemChanged(position +1);
	}

	@Override
	protected MainActivity getActivity() {
		return presenter.getActivity();
	}

	@Override
	public MessageHolder create(ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		return new MessageHolder(inflater.inflate(R.layout.activity_message, parent, false));
	}

	private int padding;
	@Override
	public void onAttachedToRecyclerView(RecyclerView recyclerView) {
		padding = recyclerView.getResources().getDimensionPixelSize(R.dimen.item_padding);
		super.onAttachedToRecyclerView(recyclerView);
	}

	public class MessageHolder extends BasicViewHolder implements View.OnClickListener {
		private final View header;
		private final TextView message;
		private final TextView name;
		private final ImageView icon;
		private final TimestampView age;

		private Subscriber subscriber;
		private long offset;

		public MessageHolder(View itemView) {
			super(itemView);
			this.header = this.itemView.findViewById(R.id.header);
			this.message = (TextView) this.itemView.findViewById(R.id.message);
			this.name = (TextView) this.itemView.findViewById(R.id.name);
			this.icon = (ImageView) this.itemView.findViewById(R.id.identicon);
			this.age = (TimestampView) this.itemView.findViewById(R.id.age);
			this.itemView.setOnClickListener(this);
		}

		public void bind(MeshMBActivityMessage prev, MeshMBActivityMessage item) {
			this.subscriber = item.subscriber;
			this.offset = item.offset;
			message.setText(item.text);
			name.setText(item.name);

			boolean prevDifferent = prev==null || !prev.subscriber.equals(item.subscriber);
			age.setDates(item.date, prev==null ? null : prev.date);

			if (prevDifferent)
				icon.setImageDrawable(new Identicon(item.subscriber.signingKey));
			header.setVisibility( prevDifferent ? View.VISIBLE : View.GONE);
			RecyclerView.LayoutParams parms = (RecyclerView.LayoutParams) itemView.getLayoutParams();
			parms.topMargin = prevDifferent ? padding : 0;
		}

		@Override
		public void onClick(View v) {
			presenter.openFeed(subscriber, offset);
		}
	}
}
