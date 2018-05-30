package org.servalproject.servalchat.navigation;

import android.graphics.drawable.Drawable;

import org.servalproject.mid.IObserverSet;

public abstract class NavTitle<T extends NavTitle> {
	protected IObserverSet<T> observers;

	public abstract Drawable getIcon();

	public CharSequence getMenuLabel(){
		return getTitle();
	}

	public abstract CharSequence getTitle();
}
