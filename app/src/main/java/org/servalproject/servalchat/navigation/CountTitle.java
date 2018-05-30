package org.servalproject.servalchat.navigation;

import android.graphics.drawable.Drawable;

import org.servalproject.mid.Messaging;
import org.servalproject.mid.ObserverProxy;
import org.servalproject.mid.ObserverSet;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.navigation.NavTitle;

public abstract class CountTitle<T> extends NavTitle {
	private final CharSequence prefix;

	public CountTitle(ObserverSet<T> observer, CharSequence prefix) {
		observers = new ObserverProxy<>(Serval.getInstance().uiHandler, observer, this);
		this.prefix = prefix;
	}

	@Override
	public Drawable getIcon() {
		return null;
	}

	public abstract int unreadCount();

	@Override
	public CharSequence getMenuLabel() {
		return prefix+" ("+unreadCount()+")";
	}

	@Override
	public CharSequence getTitle() {
		return prefix;
	}
}
