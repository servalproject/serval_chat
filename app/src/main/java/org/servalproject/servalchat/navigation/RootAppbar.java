package org.servalproject.servalchat.navigation;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Peer;
import org.servalproject.servalchat.R;

/**
 * Created by jeremy on 6/11/17.
 */

public class RootAppbar extends CoordinatorLayout implements IRootContainer, INavigate {
	private ViewGroup rootLayout;
	Toolbar toolbar;
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
		for(ILifecycle l : state.getLifecycle()) {
			if (visible)
				l.onHidden();
			l.onDetach(configChange);
		}
		rootLayout.removeView(state.view);
	}

	@Override
	public ViewState activate(Navigation n, Identity identity, Peer peer, Bundle args, boolean visible) {
		ViewState ret = ViewState.Inflate(activity, n, identity, peer, args);
		ret.view.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		rootLayout.addView(ret.view);
		for(ILifecycle l : ret.getLifecycle())
			l.onVisible();

		activity.setSupportActionBar(toolbar);
		NavTitle title = n.getTitle(activity, identity, peer);
		toolbar.setTitle(title.getTitle());
		if (Build.VERSION.SDK_INT>=21) {
			activity.setTaskDescription(new ActivityManager.TaskDescription(title.toString(), identity == null ? null : identity.getBitmap()));
		}

		return ret;
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args) {
		this.activity = activity;
		rootLayout = (ViewGroup) findViewById(R.id.root_layout);
		toolbar = (Toolbar) findViewById(R.id.app_toolbar);
		return null;
	}

	@Override
	public Toolbar getToolbar() {
		return toolbar;
	}

	@Override
	public CoordinatorLayout getCoordinator() {
		return (CoordinatorLayout)findViewById(R.id.coordinator);
	}

	@Override
	public void updateToolbar(boolean canGoBack) {
		ActionBar bar = activity.getSupportActionBar();
		bar.setDisplayOptions(
				ActionBar.DISPLAY_SHOW_HOME | (canGoBack ? ActionBar.DISPLAY_HOME_AS_UP : 0),
				ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				return activity.goBack();
		}
		return false;
	}
}
