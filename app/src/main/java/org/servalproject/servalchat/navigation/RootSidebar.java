package org.servalproject.servalchat.navigation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Observer;
import org.servalproject.mid.Peer;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.views.UIObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 6/11/17.
 */

public class RootSidebar
		extends DrawerLayout
		implements IRootContainer, INavigate, NavigationView.OnNavigationItemSelectedListener,
			IOnBack, View.OnClickListener, ILifecycle {
	private MainActivity activity;
	private RootAppbar root;
	private List<ILifecycle> lifecycles = new ArrayList<>();
	private NavigationView navigationView;
	private Navigation navigation;
	private ActionBarDrawerToggle toggle;

	public RootSidebar(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.dialogPreferenceStyle);
	}

	public RootSidebar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args) {
		this.navigation = n;
		this.activity = activity;
		root = (RootAppbar)findViewById(R.id.coordinator);
		ILifecycle rootLifeCycle = root.onAttach(activity, n, id, peer, args);
		if (rootLifeCycle !=null)
			lifecycles.add(rootLifeCycle);

		toggle = new ActionBarDrawerToggle(
				activity, this, root.toolbar,
				R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		addDrawerListener(toggle);
		toggle.syncState();

		navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		View header = navigationView.getHeaderView(0);
		header.setOnClickListener(this);
		ImageView identicon = (ImageView)header.findViewById(R.id.identicon);
		TextView name = (TextView)header.findViewById(R.id.name);

		Menu items = navigationView.getMenu();
		for (int i = 0; i < n.children.size(); i++){
			Navigation item = n.children.get(i);
			final NavTitle navTitle = item.getTitle(activity, id, peer);

			CharSequence title = navTitle.getMenuLabel();
			Drawable icon = navTitle.getIcon();

			if (item.isHeader()){
				if (icon != null)
					identicon.setImageDrawable(icon);
				name.setText(title);
			}else{
				final MenuItem menuItem = items.add(Menu.NONE, i, Menu.NONE, title);
				if (icon != null)
					menuItem.setIcon(icon);
				if (navTitle.observers!=null) {
					NavObserver o = new NavObserver(navTitle, menuItem);
					lifecycles.add(o);
				}
			}
		}

		return this;
	}

	private class NavObserver extends UIObserver<NavTitle> {
		private final MenuItem item;
		private NavObserver(NavTitle title, MenuItem item){
			super(title.observers);
			this.item = item;
		}

		@Override
		public void updated(NavTitle obj) {
			item.setTitle(obj.getMenuLabel());
			item.setIcon(obj.getIcon());
		}
	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		closeDrawer(Gravity.START);
		Navigation n = navigation.children.get(item.getItemId());
		activity.go(n, true);
		return true;
	}

	@Override
	public void deactivate(ViewState state, boolean configChange, boolean visible) {
		if (isDrawerOpen(Gravity.START))
			closeDrawer(Gravity.START, false);
		root.deactivate(state, configChange, visible);
	}

	@Override
	public ViewState activate(Navigation n, Identity identity, Peer peer, Bundle args, boolean visible) {
		if (isDrawerOpen(Gravity.START))
			closeDrawer(Gravity.START, false);
		return root.activate(n, identity, peer, args, visible);
	}

	@Override
	public boolean onBack() {
		if (isDrawerOpen(Gravity.START)){
			closeDrawer(Gravity.START);
			return true;
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		closeDrawer(Gravity.START);
		activity.go(Navigation.IdentityDetails, true);
	}

	@Override
	public Toolbar getToolbar() {
		return root.toolbar;
	}

	@Override
	public CoordinatorLayout getCoordinator() {
		return (CoordinatorLayout)findViewById(R.id.coordinator);
	}

	@Override
	public void updateToolbar(boolean canGoBack) {
		// Noop
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return toggle.onOptionsItemSelected(item);
	}

	@Override
	public void onDetach(boolean configChange) {
		for (ILifecycle l: lifecycles) {
			l.onDetach(configChange);
		}
	}

	@Override
	public void onVisible() {
		for (ILifecycle l: lifecycles) {
			l.onVisible();
		}
	}

	@Override
	public void onHidden() {
		for (ILifecycle l: lifecycles) {
			l.onHidden();
		}
	}
}
