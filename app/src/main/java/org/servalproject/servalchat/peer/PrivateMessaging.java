package org.servalproject.servalchat.peer;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
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
 * Created by jeremy on 27/07/16.
 */
public class PrivateMessaging extends RelativeLayout
		implements INavigate, View.OnClickListener {
	MainActivity activity;
	EditText message;
	Button send;
	RecyclerView list;
	PrivateMessagingPresenter presenter;

	public PrivateMessaging(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args) {
		this.activity = activity;
		this.message = (EditText) findViewById(R.id.message);
		this.send = (Button) findViewById(R.id.send);
		this.list = (RecyclerView) findViewById(R.id.message_list);
		RecyclerHelper.createLayoutManager(list, true, true);
		send.setOnClickListener(this);
		presenter = PrivateMessagingPresenter.factory.getPresenter(this, id, peer, args);
		return presenter;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.send)
			presenter.send();
	}
}
