package org.servalproject.servalchat.identity;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Peer;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;

/**
 * Created by jeremy on 7/06/16.
 */
public class IdentityDetails extends LinearLayout
		implements INavigate, View.OnClickListener {
	MainActivity activity;
	ImageView icon;
	TextView sidLabel;
	TextView sid;
	EditText name;
	Button update;
	IdentityDetailsPresenter presenter;

	public IdentityDetails(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args) {
		this.activity = activity;
		icon = (ImageView) findViewById(R.id.identicon);
		sidLabel = (TextView) findViewById(R.id.sid_label);
		sid = (TextView) findViewById(R.id.sid);
		name = (EditText) findViewById(R.id.name);
		update = (Button) findViewById(R.id.update);
		update.setOnClickListener(this);

		presenter = IdentityDetailsPresenter.factory.getPresenter(this, id, peer, args);
		return presenter;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.update:
				presenter.update();
		}
	}

}