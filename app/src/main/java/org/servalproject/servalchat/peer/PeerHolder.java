package org.servalproject.servalchat.peer;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.servalproject.mid.KnownPeers;
import org.servalproject.mid.Peer;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.BasicViewHolder;
import org.servalproject.servalchat.views.Identicon;
import org.servalproject.servaldna.SigningKey;

/**
 * Created by jeremy on 30/05/17.
 */
public class PeerHolder extends BasicViewHolder implements View.OnClickListener {
	private final ImageView icon;
	private final TextView name;
	private final MainActivity activity;
	private Peer peer;

	public PeerHolder(MainActivity activity, ViewGroup parent) {
		super(LayoutInflater.from(parent.getContext()).inflate(R.layout.peer, parent, false));
		this.activity = activity;
		//avatar = (ImageView)this.itemView.findViewById(R.id.avatar);
		name = (TextView) this.itemView.findViewById(R.id.name);
		icon = (ImageView) this.itemView.findViewById(R.id.identicon);
		this.itemView.setOnClickListener(this);
	}

	public void bind(Peer peer) {
		this.peer = peer;
		this.name.setText(peer.displayName());
		SigningKey key = peer.getSubscriber().signingKey;
		if (key == null){
			this.icon.setVisibility(View.INVISIBLE);
		}else{
			this.icon.setImageDrawable(new Identicon(key));
			this.icon.setVisibility(View.VISIBLE);
		}

		if (peer.isReachable()) {
			// Show green dot?
		}
	}

	@Override
	public void onClick(View v) {
		activity.go(Navigation.PeerDetails, peer, null);
	}
}
