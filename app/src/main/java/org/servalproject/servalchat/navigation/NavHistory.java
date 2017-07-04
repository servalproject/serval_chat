package org.servalproject.servalchat.navigation;

import android.os.Bundle;

import org.servalproject.servaldna.AbstractId;
import org.servalproject.servaldna.SigningKey;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.SubscriberId;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 19/07/16.
 */
public class NavHistory {
	public final SigningKey identity;
	public final List<HistoryItem> history = new ArrayList<>();
	private static final String TAG = "History";

	private Navigation getDefault(){
		return identity == null ? Navigation.IdentityList : Navigation.MyFeed;
	}

	public HistoryItem getTop() {
		if (history.size() == 0)
			history.add(new HistoryItem(getDefault(), identity, null, null));
		return history.get(history.size() - 1);
	}

	private NavHistory(SigningKey identity) {
		this.identity = identity;
	}

	private void pop() {
		history.remove(history.size() - 1);
	}

	public boolean canGoBack(){
		return history.size()>1;
	}

	public boolean back() {
		if (history.size() <= 1)
			return false;
		pop();
		return true;
	}

	public boolean add(HistoryItem item, boolean replace) {
		HistoryItem top = getTop();
		// Ignore if we haven't gone anywhere
		if (top.equals(item))
			return false;
		if (replace)
			pop();
		history.add(item);
		return true;
	}

	public boolean add(Navigation key, SigningKey identity, Subscriber peer, Bundle args, boolean replace) {
		return add(new HistoryItem(key, identity, peer, args), replace);
	}

	private static void save(Bundle state, Navigation key, Subscriber peer, Bundle args, int i){
		state.putString("history.key_" + i, key.name);
		if (peer != null) {
			state.putByteArray("history.peer_sid" + i, peer.sid.getBinary());
			if (peer.signingKey!=null) {
				state.putByteArray("history.peer_sign" + i, peer.signingKey.getBinary());
				state.putBoolean("history.peer_combined" + i, peer.combined);
			}
		}
		if (args != null)
			state.putBundle("history.args_" + i, args);
	}

	public void save(Bundle state) {
		if (identity != null)
			state.putByteArray("history.identity", identity.getBinary());

		for (int i = 0; i < history.size(); i++) {
			HistoryItem item = history.get(i);
			save(state, item.key, item.peer, item.args, i);
		}
	}

	public static Bundle prepareNew(Navigation key, SigningKey identity, Subscriber peer, Bundle args) {
		Bundle state = new Bundle();
		if (identity != null)
			state.putByteArray("history.identity", identity.getBinary());
		int i=0;
		if (key.defaultParent!=null)
			state.putString("history.key_" + (i++), key.defaultParent.name);
		save(state, key, peer, args, i);
		return state;
	}

	public static NavHistory restore(Bundle state) {
		try {
			if (state == null)
				return new NavHistory(null);

			byte[] id = state.getByteArray("history.identity");
			SigningKey identity = null;
			if (id != null)
				identity = new SigningKey(id);

			NavHistory ret = new NavHistory(identity);
			for (int i = 0; ; i++) {
				String key = state.getString("history.key_" + i);
				if (key == null)
					break;
				Navigation nav = Navigation.NavMap.get(key);
				if (nav == null)
					throw new IllegalStateException();
				byte sid[] = state.getByteArray("history.peer_sid" + i);
				Subscriber peer = null;
				if (sid!=null)
					peer = new Subscriber(sid,
							state.getByteArray("history.peer_sign" + i),
							state.getBoolean("history.peer_combined" + i, false));
				Bundle args = state.getBundle("history.args_" + i);
				ret.history.add(new HistoryItem(nav, identity, peer, args));
			}
			return ret;
		} catch (AbstractId.InvalidBinaryException e) {
			throw new IllegalStateException(e);
		}
	}
}
