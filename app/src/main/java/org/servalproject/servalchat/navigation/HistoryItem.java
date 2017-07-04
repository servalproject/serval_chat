package org.servalproject.servalchat.navigation;

import android.os.Bundle;

import org.servalproject.servaldna.SigningKey;
import org.servalproject.servaldna.Subscriber;

/**
 * Created by jeremy on 20/07/16.
 */
public class HistoryItem {
	public final Navigation key;
	public final SigningKey identity;
	public final Subscriber peer;
	public final Bundle args;

	public HistoryItem(Navigation key, SigningKey identity, Subscriber peer, Bundle args) {
		this.key = key;
		this.args = args;
		this.identity = identity;
		this.peer = peer;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		HistoryItem that = (HistoryItem) o;

		// TODO compare arg values?
		return key.equals(that.key) && args == that.args;
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}
}
