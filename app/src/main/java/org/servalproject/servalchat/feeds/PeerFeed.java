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
import org.servalproject.mid.Messaging;
import org.servalproject.mid.Peer;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.IHaveMenu;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.RecyclerHelper;
import org.servalproject.servaldna.meshmb.MeshMBCommon;

/**
 * Created by jeremy on 3/08/16.
 */
public class PeerFeed extends LinearLayout
		implements INavigate {

	RecyclerView list;
	PeerFeedPresenter presenter;
	MainActivity activity;

	public PeerFeed(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args) {
		this.activity = activity;
		this.list = (RecyclerView) findViewById(R.id.list);
		RecyclerHelper.createLayoutManager(list, true, false);
		return presenter = PeerFeedPresenter.factory.getPresenter(this, id, peer, args);
	}

}
