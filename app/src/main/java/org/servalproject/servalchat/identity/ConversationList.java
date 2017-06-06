package org.servalproject.servalchat.identity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.KnownPeers;
import org.servalproject.mid.Messaging;
import org.servalproject.mid.Observer;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.IHaveMenu;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.ObservedRecyclerView;
import org.servalproject.servalchat.views.RecyclerHelper;
import org.servalproject.servaldna.meshms.MeshMSConversation;

import java.util.List;

/**
 * Created by jeremy on 13/07/16.
 */
public class ConversationList
		extends ObservedRecyclerView<MeshMSConversation, ConversationList.ConversationHolder>
		implements INavigate, IHaveMenu, MenuItem.OnMenuItemClickListener {

	private Navigation navigation;
	private List<MeshMSConversation> conversations;
	private static final String TAG = "ConversationList";
	private final Serval serval;

	public ConversationList(Context context, @Nullable AttributeSet attrs) {
		super(null, context, attrs);
		this.serval = Serval.getInstance();
		setHasFixedSize(true);
		RecyclerHelper.createLayoutManager(this, true, false);
		RecyclerHelper.createDivider(this);
	}


	@Override
	protected ConversationHolder createHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_item, parent, false);
		return new ConversationHolder(view);
	}

	@Override
	protected void bind(ConversationHolder holder, MeshMSConversation item) {
		holder.setConversation(item);
	}

	@Override
	protected void unBind(ConversationHolder holder, MeshMSConversation item) {
		holder.setConversation(null);
	}

	@Override
	protected MeshMSConversation get(int position) {
		if (position >= getCount())
			return null;
		return conversations.get(position);
	}

	@Override
	protected int getCount() {
		if (conversations == null)
			return 0;
		return conversations.size();
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args) {
		List<MeshMSConversation> list;
		if (n == Navigation.Inbox)
			list = id.messaging.conversations;
		else if(n == Navigation.Requests)
			list = id.messaging.requests;
		else
			throw new IllegalStateException("Unexpected navigation!");

		this.navigation = n;
		if (this.conversations != list) {
			this.conversations = list;
			this.setObserverSet(id.messaging.observers);
			notifyChanged();
		}
		return super.onAttach(activity, n, id, args);
	}

	private static final int REQUESTS = 1;
	private static final int BLOCKED = 2;

	@Override
	public void populateItems(Menu menu) {
		if (navigation == Navigation.Inbox){
			// TODO show unread count?
			menu.add(Menu.NONE, REQUESTS, Menu.NONE, R.string.requests)
					.setOnMenuItemClickListener(this);
			// TODO
//			menu.add(Menu.NONE, BLOCKED, Menu.NONE, R.string.blocked)
//					.setOnMenuItemClickListener(this);
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()){
			case REQUESTS:
				activity.go(identity, Navigation.Requests, null);
				return true;
			case BLOCKED:
				activity.go(identity, Navigation.Blocked, null);
				return true;
		}
		return false;
	}

	public class ConversationHolder
			extends RecyclerView.ViewHolder
			implements View.OnClickListener, Observer<Peer> {
		final TextView name;
		private MeshMSConversation conversation;
		private Peer peer;

		public ConversationHolder(View itemView) {
			super(itemView);
			name = (TextView) this.itemView.findViewById(R.id.name);
			itemView.setOnClickListener(this);
		}

		public void setConversation(MeshMSConversation conversation) {
			this.conversation = conversation;
			Peer p = null;
			if (conversation != null)
				p = serval.knownPeers.getPeer(conversation.them);
			if (this.peer != p) {
				if (this.peer != null)
					this.peer.observers.removeUI(this);
				this.peer = p;
				if (p != null)
					this.peer.observers.addUI(this);
			}
			if (p != null)
				updated(p);
		}

		@Override
		public void onClick(View v) {
			Bundle args = new Bundle();
			KnownPeers.saveSubscriber(conversation.them, args);
			activity.go(identity, Navigation.PrivateMessages, args);
		}

		@Override
		public void updated(Peer obj) {
			name.setText(obj.displayName());
		}
	}
}
