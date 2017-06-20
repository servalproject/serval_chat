package org.servalproject.servalchat.feeds;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.util.SortedList;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.util.AttributeSet;
import android.view.ViewGroup;

import org.servalproject.mid.Identity;
import org.servalproject.mid.ListObserver;
import org.servalproject.mid.Peer;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.peer.PeerHolder;
import org.servalproject.servalchat.views.RecyclerHelper;
import org.servalproject.servalchat.views.SimpleRecyclerView;

/**
 * Created by jeremy on 19/06/17.
 */

public class BlockList extends SimpleRecyclerView<Peer, PeerHolder>
		implements ILifecycle, INavigate, ListObserver<Peer> {
	private MainActivity activity;
	private final SortedList<Peer> list;
	private Identity identity;
	private int generation = -1;

	public BlockList(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		listAdapter.setHasStableIds(true);
		setHasFixedSize(true);
		RecyclerHelper.createLayoutManager(this, true, false);
		RecyclerHelper.createDivider(this);
		SortedListAdapterCallback<Peer> listCallback = new SortedListAdapterCallback<Peer>(listAdapter) {
			@Override
			public int compare(Peer o1, Peer o2) {
				return o1.compareTo(o2);
			}

			@Override
			public boolean areContentsTheSame(Peer oldItem, Peer newItem) {
				return oldItem.equals(newItem);
			}

			@Override
			public boolean areItemsTheSame(Peer item1, Peer item2) {
				return item1.equals(item2);
			}
		};
		list = new SortedList<Peer>(Peer.class, listCallback);
	}

	@Override
	protected PeerHolder createHolder(ViewGroup parent, int viewType) {
		return new PeerHolder(activity, parent);
	}

	@Override
	protected void bind(PeerHolder holder, Peer item) {
		holder.bind(item);
	}

	@Override
	protected Peer get(int position) {
		return list.get(position);
	}

	@Override
	protected int getCount() {
		return list.size();
	}

	@Override
	public void added(Peer obj) {
		list.add(obj);
	}

	@Override
	public void removed(Peer obj) {
		list.remove(obj);
	}

	@Override
	public void updated(Peer obj) {
	}

	@Override
	public void reset() {
		list.beginBatchedUpdates();
		list.clear();
		list.addAll(identity.messaging.getBlockList());
		list.endBatchedUpdates();
	}

	@Override
	public void onVisible() {
		int g = identity.messaging.observeBlockList.add(this);
		if (g != generation)
			reset();
	}

	@Override
	public void onHidden() {
		generation = identity.messaging.observeBlockList.remove(this);
	}

	@Override
	public void onDetach(boolean configChanging) {

	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args) {
		this.activity = activity;
		this.identity = id;
		return this;
	}

}
