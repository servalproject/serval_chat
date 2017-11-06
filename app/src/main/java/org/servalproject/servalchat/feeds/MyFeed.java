package org.servalproject.servalchat.feeds;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Peer;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.RecyclerHelper;

/**
 * Created by jeremy on 8/08/16.
 */
public class MyFeed extends RelativeLayout
		implements INavigate, View.OnClickListener {
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.post:
				presenter.post();
		}
	}
}
