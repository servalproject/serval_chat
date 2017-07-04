package org.servalproject.servalchat.feeds;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Peer;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.IHaveMenu;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.RecyclerHelper;

/**
 * Created by jeremy on 8/08/16.
 */
public class MyFeed extends RelativeLayout
		implements INavigate, IHaveMenu, MenuItem.OnMenuItemClickListener, View.OnClickListener {
	Button post;
	EditText message;
	RecyclerView list;
	MyFeedPresenter presenter;
	MainActivity activity;

	public MyFeed(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args) {
		this.activity = activity;
		this.post = (Button) findViewById(R.id.post);
		this.post.setOnClickListener(this);
		this.message = (EditText) findViewById(R.id.message);
		this.list = (RecyclerView) findViewById(R.id.activity);
		RecyclerHelper.createLayoutManager(list, true, false);
		RecyclerHelper.createDivider(list);
		return presenter = MyFeedPresenter.factory.getPresenter(this, id, peer, args);
	}

	private static final int ALL_FEEDS=1;
	private static final int CONTACTS=2;
	private static final int BLOCKED=3;

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		switch(menuItem.getItemId()){
			case ALL_FEEDS:
				activity.go(Navigation.AllFeeds);
				return true;
			case CONTACTS:
				activity.go(Navigation.Contacts);
				return true;
			case BLOCKED:
				activity.go(Navigation.Blocked);
				return true;
		}
		return false;
	}

	@Override
	public void populateItems(Menu menu) {
		menu.add(Menu.NONE, CONTACTS, Menu.NONE, R.string.contacts)
				.setOnMenuItemClickListener(this);
		menu.add(Menu.NONE, ALL_FEEDS, Menu.NONE, R.string.all_feeds)
				.setOnMenuItemClickListener(this);
		menu.add(Menu.NONE, BLOCKED, Menu.NONE, R.string.blocked)
				.setOnMenuItemClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.post:
				presenter.post();
		}
	}
}
