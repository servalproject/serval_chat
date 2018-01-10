package org.servalproject.servalchat.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * Created by jeremy on 11/07/16.
 */
public abstract class SimpleRecyclerView<T, H extends BasicViewHolder>
		extends RecyclerView {
	protected final ListAdapter listAdapter;
	private final int emptyResource;

	public SimpleRecyclerView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, -1);
	}

	public SimpleRecyclerView(Context context, @Nullable AttributeSet attrs, int emptyResource) {
		super(context, attrs);
		this.emptyResource = emptyResource;
		listAdapter = new ListAdapter();
	}

	@Override
	protected void onAttachedToWindow() {
		setAdapter(listAdapter);
		super.onAttachedToWindow();
	}

	protected int getItemType(T item) {
		return 0;
	}

	abstract protected H createHolder(ViewGroup parent, int viewType);

	abstract protected void bind(H holder, T item);

	protected void unBind(H holder, T item) {
	}

	protected long getId(T item) {
		return -1;
	}

	abstract protected T get(int position);

	abstract protected int getCount();

	protected void notifyChanged() {
		listAdapter.notifyDataSetChanged();
	}

	public class ListAdapter extends RecyclerView.Adapter<BasicViewHolder> {
		@Override
		public BasicViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType == 0)
				return new MessageViewHolder(emptyResource, parent);
			return createHolder(parent, viewType -1);
		}

		@Override
		public void onViewRecycled(BasicViewHolder holder) {
			if (holder instanceof MessageViewHolder)
				return;
			int position = holder.getAdapterPosition();
			if (position != NO_POSITION)
				//noinspection unchecked
				unBind((H)holder, get(position));
		}

		@Override
		public void onBindViewHolder(BasicViewHolder holder, int position) {
			if (holder instanceof MessageViewHolder)
				return;
			//noinspection unchecked
			bind((H)holder, get(position));
		}

		@Override
		public int getItemCount() {
			int count = getCount();
			if (count == 0 && emptyResource!=-1)
				return 1;
			return getCount();
		}

		public long getItemId(int position) {
			if (getCount()==0)
				return -1;
			return getId(get(position));
		}

		@Override
		public int getItemViewType(int position) {
			if (getCount()==0)
				return 0;
			return getItemType(get(position))+1;
		}
	}

}
