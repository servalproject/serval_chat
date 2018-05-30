package org.servalproject.servalchat.navigation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Peer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 19/07/16.
 */
public class ViewState {
	public final Navigation key;
	private IContainerView container;
	private List<ILifecycle> lifecycle = new ArrayList<>();
	public final View view;
	private View firstInput = null;

	public IContainerView getContainer() {
		return container;
	}

	public List<ILifecycle> getLifecycle() {
		return lifecycle;
	}

	public View getTextInput(){
		return firstInput;
	}

	public interface ViewVisitor{
		boolean visit(View view);
	}

	public boolean visitViews(View view, ViewVisitor visitor){
		if (visitor.visit(view))
			return true;
		if (view instanceof ViewGroup) {
			ViewGroup g = (ViewGroup) view;
			for (int i = 0; i < g.getChildCount(); i++) {
				if (visitViews(g.getChildAt(i), visitor))
					return true;
			}
		}
		return false;
	}

	public boolean visit(ViewVisitor visitor){
		return visitViews(view, visitor);
	}

	public static ViewState Inflate(final MainActivity activity, final Navigation key, final Identity identity, final Peer peer, final Bundle args) {
		LayoutInflater inflater = LayoutInflater.from(activity);
		View view = inflater.inflate(key.layoutResource, null);
		final ViewState ret = new ViewState(key, view);
		ret.visit(new ViewVisitor() {
			@Override
			public boolean visit(View view) {
				if (view instanceof INavigate) {
					INavigate navigate = (INavigate) view;
					ILifecycle l = navigate.onAttach(activity, key, identity, peer, args);
					if (l!=null)
						ret.lifecycle.add(l);
				}
				if (view.onCheckIsTextEditor() && ret.firstInput == null)
					ret.firstInput = view;
				if (view instanceof IContainerView && ret.container == null)
					ret.container = (IContainerView) view;
				return false;
			}
		});
		return ret;
	}

	private ViewState(Navigation key, View view) {
		this.key = key;
		this.view = view;
	}
}
