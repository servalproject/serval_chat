package org.servalproject.servalchat.feeds;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;

import org.servalproject.mid.Identity;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.IHaveMenu;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.RecyclerHelper;

/**
 * Created by jeremy on 30/01/17.
 */

public class Activity  extends RecyclerView
		implements INavigate, IHaveMenu, MenuItem.OnMenuItemClickListener {
	ActivityPresenter presenter;
	MainActivity activity;

	public Activity(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args) {
		this.activity = activity;
		RecyclerHelper.createLayoutManager(this, true, false);
		RecyclerHelper.createDivider(this);
		presenter = ActivityPresenter.factory.getPresenter(this, id, args);
		return presenter;
	}

	private static final int ALL_FEEDS=1;

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		switch(menuItem.getItemId()){
			case ALL_FEEDS:
				activity.go(Navigation.AllFeeds, null);
				return true;
		}
		return false;
	}

	@Override
	public void populateItems(Menu menu) {
		menu.add(Menu.NONE, ALL_FEEDS, Menu.NONE, R.string.all_feeds)
				.setOnMenuItemClickListener(this);
	}
}
