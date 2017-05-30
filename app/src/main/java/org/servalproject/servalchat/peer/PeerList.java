package org.servalproject.servalchat.peer;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import org.servalproject.mid.ListObserverSet;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.IHaveMenu;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.ObservedRecyclerView;
import org.servalproject.servalchat.views.RecyclerHelper;
import org.servalproject.servaldna.SubscriberId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by jeremy on 31/05/16.
 */
public class PeerList extends ObservedRecyclerView<Peer, PeerHolder>
		implements IHaveMenu, MenuItem.OnMenuItemClickListener {
	private Serval serval;
	private static final String TAG = "PeerList";
	private List<Peer> items = new ArrayList<Peer>();

	private static ListObserverSet<Peer> getObserver() {
		Serval serval = Serval.getInstance();
		if (serval == null)
			return null;
		return serval.knownPeers.peerListObservers;
	}

	public PeerList(Context context, @Nullable AttributeSet attrs) {
		super(getObserver(), context, attrs);
		listAdapter.setHasStableIds(true);
		serval = Serval.getInstance();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		setHasFixedSize(true);
		RecyclerHelper.createLayoutManager(this, true, false);
		RecyclerHelper.createDivider(this);
	}

	@Override
	public PeerHolder createHolder(ViewGroup parent, int viewType) {
		return new PeerHolder(activity, parent);
	}

	@Override
	public void bind(PeerHolder holder, Peer item) {
		holder.bind(item);
	}

	@Override
	public long getId(Peer item) {
		return item.getId();
	}

	private static final int MAP = 1;

	@Override
	public void populateItems(Menu menu) {
		menu.add(Menu.NONE, MAP, Menu.NONE, R.string.peer_map)
				.setOnMenuItemClickListener(this);
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		switch(menuItem.getItemId()) {
			case MAP:
				activity.go(identity, Navigation.PeerMap, null);
				return true;
		}
		return false;
	}

	private boolean sorted = false;
	private final Set<SubscriberId> addedPeers = new HashSet<>();

	@Override
	public void onVisible() {
		addedPeers.clear();
		items.clear();
		if (serval != null) {
			for (Peer p : serval.knownPeers.getReachablePeers())
				add(p);
		}
		super.onVisible();
	}

	@Override
	public void onHidden() {
		super.onHidden();
		addedPeers.clear();
		items.clear();
	}

	private boolean add(Peer p) {
		if (!p.isReachable())
			return false;
		if (addedPeers.contains(p.getSubscriber().sid))
			return false;
		addedPeers.add(p.getSubscriber().sid);
		items.add(p);
		sorted = false;
		return true;
	}

	@Override
	protected Peer get(int position) {
		sort();
		return items.get(position);
	}

	@Override
	protected int getCount() {
		return items.size();
	}

	private void sort() {
		if (sorted)
			return;
		Collections.sort(items);
		sorted = true;
	}

	@Override
	public void added(Peer obj) {
		if (add(obj))
			super.added(obj);
	}

	@Override
	public void updated(Peer obj) {
		add(obj);
		sorted = false;
		super.updated(obj);
	}

}
