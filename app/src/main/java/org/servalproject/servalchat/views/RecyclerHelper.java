package org.servalproject.servalchat.views;

import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by jeremy on 14/11/16.
 */

public class RecyclerHelper {
	private RecyclerHelper(){}

	public static void createLayoutManager(RecyclerView view, boolean vertical, boolean reverse){
		LinearLayoutManager layoutManager = new LinearLayoutManager(
				view.getContext(),
				vertical?LinearLayoutManager.VERTICAL:LinearLayoutManager.HORIZONTAL,
				reverse);
		view.setLayoutManager(layoutManager);
	}

	public static void createDivider(RecyclerView view){
		LinearLayoutManager layoutManager = (LinearLayoutManager)view.getLayoutManager();
		view.addItemDecoration(new DividerItemDecoration(view.getContext(), layoutManager.getOrientation()));
	}

}
