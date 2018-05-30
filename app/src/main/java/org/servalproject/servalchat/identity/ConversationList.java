package org.servalproject.servalchat.identity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Observer;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.BasicViewHolder;
import org.servalproject.servalchat.views.Identicon;
import org.servalproject.servalchat.views.ObservedRecyclerView;
import org.servalproject.servalchat.views.RecyclerHelper;
import org.servalproject.servaldna.SigningKey;
import org.servalproject.servaldna.meshms.MeshMSConversation;

import java.util.List;

/**
 * Created by jeremy on 13/07/16.
 */
public class ConversationList
		extends ObservedRecyclerView<MeshMSConversation, ConversationList.ConversationHolder>
		implements INavigate {

	private Navigation navigation;
	private List<MeshMSConversation> conversations;
	private static final String TAG = "ConversationList";
	private final Serval serval;

	public ConversationList(Context context, @Nullable AttributeSet attrs) {
		super(null, context, attrs, R.string.empty_conversation_list);
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
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args) {
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
			this.setObserverSet(id.messaging.observeConversations);
			notifyChanged();
		}
		return super.onAttach(activity, n, id, peer, args);
	}

	public class ConversationHolder
			extends BasicViewHolder
			implements View.OnClickListener, Observer<Peer> {
		final TextView name;
		final ImageView icon;
		private MeshMSConversation conversation;
		private Peer peer;

		public ConversationHolder(View itemView) {
			super(itemView);
			name = (TextView) this.itemView.findViewById(R.id.name);
			icon = (ImageView) this.itemView.findViewById(R.id.identicon);
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
				SigningKey key = p==null ? null : p.getSubscriber().signingKey;
				if (key != null)
					icon.setImageDrawable(new Identicon(key));
				icon.setVisibility(key == null ? INVISIBLE : VISIBLE);
				if (p != null)
					this.peer.observers.addUI(this);
			}
			if (p != null)
				updated(p);
		}

		@Override
		public void onClick(View v) {
			Peer peer = serval.knownPeers.getPeer(conversation.them);
			activity.go(Navigation.PrivateMessages, peer, null);
		}

		@Override
		public void updated(Peer obj) {
			name.setText(obj.displayName());
		}
	}
}
