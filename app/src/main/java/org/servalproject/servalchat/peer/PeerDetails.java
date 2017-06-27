package org.servalproject.servalchat.peer;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Observer;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.IHaveMenu;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.Identicon;
import org.servalproject.servaldna.AbstractId;
import org.servalproject.servaldna.SigningKey;

/**
 * Created by jeremy on 1/08/16.
 */
public class PeerDetails extends LinearLayout
		implements INavigate, ILifecycle, Observer<Peer> {

	private MainActivity activity;
	private ImageView icon;
	private TextView name;
	private TextView number;
	private TextView numberLabel;
	private TextView sid;
	private Peer peer;

	public PeerDetails(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args) {
		this.activity = activity;
		icon = (ImageView) findViewById(R.id.identicon);
		name = (TextView) findViewById(R.id.name);
		number = (TextView) findViewById(R.id.number);
		numberLabel = (TextView) findViewById(R.id.number_label);
		sid = (TextView) findViewById(R.id.sid);

		try {
			Serval serval = Serval.getInstance();
			this.peer = serval.knownPeers.getPeer(args);
			updated(this.peer);
		} catch (AbstractId.InvalidBinaryException e) {
			throw new IllegalStateException();
		}

		return this;
	}

	@Override
	public void onDetach(boolean configChange) {

	}

	@Override
	public void onVisible() {
		peer.observers.addUI(this);
		updated(peer);
	}

	@Override
	public void onHidden() {
		peer.observers.removeUI(this);
	}

	@Override
	public void updated(Peer obj) {
		name.setText(peer.getName());
		String did =peer.getDid();
		number.setText(did);
		SigningKey key = peer.getSubscriber().signingKey;
		if (icon.getDrawable() == null && key!=null)
			icon.setImageDrawable(new Identicon(key));
		number.setVisibility( (did==null || "".equals(did)) ? GONE : VISIBLE);
		numberLabel.setVisibility( (did==null || "".equals(did)) ? GONE : VISIBLE);

		sid.setText(peer.getSubscriber().sid.toHex());
	}
}
