package org.servalproject.servalchat.navigation;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Peer;
import org.servalproject.servalchat.R;

/**
 * Created by jeremy on 6/11/17.
 */

public class RootAppbar extends CoordinatorLayout implements IContainerView, INavigate {
	private ViewGroup rootLayout;
	private Toolbar toolbar;
	private MainActivity activity;
	private static final String TAG = "RootAppbar";

	public RootAppbar(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.dialogPreferenceStyle);
	}

	public RootAppbar(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void deactivate(ViewState state, boolean configChange, boolean visible) {
		ILifecycle lifecycle = state.getLifecycle();
		if (visible && lifecycle != null)
			lifecycle.onHidden();
		if (lifecycle != null)
			lifecycle.onDetach(configChange);
		rootLayout.removeView(state.view);
	}

	@Override
	public ViewState activate(Navigation n, Identity identity, Peer peer, Bundle args, boolean visible) {
		ViewState ret = ViewState.Inflate(activity, n, identity, peer, args);
		ret.view.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		rootLayout.addView(ret.view);
		ILifecycle lifecycle = ret.getLifecycle();
		if (visible && lifecycle != null)
			lifecycle.onVisible();
		return ret;
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args) {
		this.activity = activity;
		toolbar = (Toolbar) findViewById(R.id.app_toolbar);
		rootLayout = (ViewGroup) findViewById(R.id.root_layout);
		activity.setSupportActionBar(toolbar);
		CharSequence title = n.getTitle(getContext(), id, peer);
		toolbar.setTitle(title);
		if (Build.VERSION.SDK_INT>=21) {
			activity.setTaskDescription(new ActivityManager.TaskDescription(title.toString(), id == null ? null : id.getBitmap()));
		}
		return null;
	}
}
