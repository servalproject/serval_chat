package org.servalproject.servalchat.feeds;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
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
 * Created by jeremy on 11/10/16.
 */
public class PublicFeedsList extends RelativeLayout implements INavigate {
	PublicFeedsPresenter presenter;
	MainActivity activity;
	EditText search;
	RecyclerView feedList;

	public PublicFeedsList(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args) {
		this.activity = activity;
		search = findViewById(R.id.search);
		feedList = findViewById(R.id.feed_list);
		search.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				presenter.search(charSequence);
			}

			@Override
			public void afterTextChanged(Editable editable) {

			}
		});
		RecyclerHelper.createLayoutManager(feedList, true, false);
		RecyclerHelper.createDivider(feedList);
		presenter = PublicFeedsPresenter.factory.getPresenter(this, id, peer, args);
		return presenter;
	}
}
