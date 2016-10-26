package org.servalproject.servalchat.navigation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.servalproject.mid.Identity;

/**
 * Created by jeremy on 19/07/16.
 */
public class ViewState {
	public final Navigation key;
	private IContainerView container;
	private ILifecycle lifecycle;
	public final View view;

	public IContainerView getContainer() {
		return container;
	}

	public ILifecycle getLifecycle() {
		return lifecycle;
	}

	private void attach(MainActivity activity, Navigation key, Identity identity, Bundle args, View view) {
		if (view instanceof INavigate) {
			INavigate navigate = (INavigate) view;
			lifecycle = navigate.onAttach(activity, key, identity, args);
		}
		if (view instanceof IContainerView)
			container = (IContainerView) view;
		if (view instanceof ViewGroup) {
			ViewGroup g = (ViewGroup) view;
			for (int i = 0; i < g.getChildCount(); i++)
				attach(activity, key, identity, args, g.getChildAt(i));
		}
	}

	public static ViewState Inflate(MainActivity activity, Navigation key, Identity identity, Bundle args) {
		LayoutInflater inflater = LayoutInflater.from(activity);
		View view = inflater.inflate(key.layoutResource, null);
		ViewState ret = new ViewState(key, view);
		ret.attach(activity, key, identity, args, view);
		return ret;
	}

	private ViewState(Navigation key, View view) {
		this.key = key;
		this.view = view;
	}
}
