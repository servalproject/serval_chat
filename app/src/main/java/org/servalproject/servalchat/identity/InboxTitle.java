package org.servalproject.servalchat.identity;

import android.graphics.drawable.Drawable;

import org.servalproject.mid.Messaging;
import org.servalproject.mid.ObserverProxy;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.navigation.NavTitle;

public abstract class InboxTitle extends NavTitle {
	private final CharSequence prefix;
	protected final Messaging messaging;
	public InboxTitle(Messaging messaging, CharSequence prefix) {
		observers = new ObserverProxy<>(Serval.getInstance().uiHandler, messaging.observers, this);
		this.prefix = prefix;
		this.messaging = messaging;
	}

	@Override
	public Drawable getIcon() {
		return null;
	}

	public abstract int unreadCount();

	@Override
	public CharSequence getMenuLabel() {
		int count = unreadCount();
		//if (count>0)
			return prefix+" ("+count+")";
		//return prefix;
	}

	@Override
	public CharSequence getTitle() {
		return prefix;
	}
}
