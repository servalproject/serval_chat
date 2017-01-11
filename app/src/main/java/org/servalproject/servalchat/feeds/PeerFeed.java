package org.servalproject.servalchat.feeds;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import org.servalproject.mid.Identity;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.IHaveMenu;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.RecyclerHelper;

/**
 * Created by jeremy on 3/08/16.
 */
public class PeerFeed extends LinearLayout
		implements INavigate, IHaveMenu, MenuItem.OnMenuItemClickListener {

	RecyclerView list;
	PeerFeedPresenter presenter;
	MainActivity activity;

	public PeerFeed(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	private static final int FOLLOW = 1;
	private static final int IGNORE = 2;

	@Override
	public void populateItems(Menu menu) {
		menu.add(Menu.NONE, FOLLOW, Menu.NONE, R.string.follow_feed)
				.setOnMenuItemClickListener(this);
		menu.add(Menu.NONE, IGNORE, Menu.NONE, R.string.ignore_feed)
				.setOnMenuItemClickListener(this);
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
			case FOLLOW:
				presenter.subscribe(true);
				break;
			case IGNORE:
				presenter.subscribe(false);
				break;
			default:
				return false;
		}
		return true;
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args) {
		this.activity = activity;
		this.list = (RecyclerView) findViewById(R.id.list);
		RecyclerHelper.createLayoutManager(list, true, false);
		return presenter = PeerFeedPresenter.factory.getPresenter(this, id, args);
	}

}
