package org.servalproject.servalchat.identity;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.ListObserverSet;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.IHaveMenu;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.Identicon;
import org.servalproject.servalchat.views.ObservedRecyclerView;
import org.servalproject.servalchat.views.RecyclerHelper;
import org.servalproject.servaldna.keyring.KeyringIdentity;
import org.servalproject.servaldna.keyring.KeyringIdentityList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 1/06/16.
 */
public class IdentityList
		extends ObservedRecyclerView<Identity, IdentityList.IdentityHolder>
		implements IHaveMenu, MenuItem.OnMenuItemClickListener {
	private Serval serval;
	private static final String TAG = "IdentityList";
	private final List<Identity> identities;

	private static ListObserverSet<Identity> getObserver() {
		Serval serval = Serval.getInstance();
		if (serval == null) // eg design mode
			return null;
		return serval.identities.listObservers;
	}

	public IdentityList(Context context, @Nullable AttributeSet attrs) {
		super(getObserver(), context, attrs);
		serval = Serval.getInstance();
		if (serval == null) {
			// example data for editing layouts
			identities = new ArrayList<>();
			for (KeyringIdentity i : KeyringIdentityList.getTestIdentities()) {
				Identity id = new Identity(null, i.subscriber);
				id.update(i);
				identities.add(id);
			}
		} else {
			identities = serval.identities.getIdentities();
		}
		listAdapter.setHasStableIds(true);
		setHasFixedSize(true);
		RecyclerHelper.createLayoutManager(this, true, false);
		RecyclerHelper.createDivider(this);
	}

	@Override
	public IdentityHolder createHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.identity, parent, false);
		return new IdentityHolder(view);
	}

	@Override
	public void bind(IdentityHolder holder, Identity item) {
		holder.id = item;
		holder.icon.setImageDrawable(item.getIcon());

		CharSequence name = item.getName();
		if (name == null)
			name = getContext().getString(R.string.no_name);
		holder.name.setText(name);
	}

	@Override
	protected Identity get(int position) {
		return identities.get(position);
	}

	@Override
	protected int getCount() {
		return identities.size();
	}

	@Override
	public long getId(Identity item) {
		return item.getId();
	}

	private static final int ADD = 1;

	@Override
	public void populateItems(Menu menu) {
		MenuItem add = menu.add(Menu.NONE, ADD, Menu.NONE, R.string.add_identity)
				.setOnMenuItemClickListener(this)
				.setIcon(R.drawable.ic_add_circle);
		MenuItemCompat.setShowAsAction(add, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
			case ADD:
				activity.go(Navigation.NewIdentityDetails, null);
				return true;
		}
		return false;
	}

	public class IdentityHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		private final TextView name;
		private final ImageView icon;
		private Identity id;

		public IdentityHolder(View view) {
			super(view);
			name = (TextView) this.itemView.findViewById(R.id.name);
			icon = (ImageView) this.itemView.findViewById(R.id.identicon);
			this.itemView.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			activity.go(id, Navigation.MyFeed, null);
		}
	}
}
