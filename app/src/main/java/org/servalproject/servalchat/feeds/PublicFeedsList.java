package org.servalproject.servalchat.feeds;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import org.servalproject.mid.Identity;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;

/**
 * Created by jeremy on 11/10/16.
 */
public class PublicFeedsList extends RecyclerView implements INavigate {
	LinearLayoutManager layoutManager;
	PublicFeedsPresenter presenter;
	MainActivity activity;

	public PublicFeedsList(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args) {
		this.activity = activity;
		layoutManager = new LinearLayoutManager(this.getContext());
		setLayoutManager(layoutManager);
		presenter = PublicFeedsPresenter.factory.getPresenter(this, id, args);
		return presenter;
	}
}
