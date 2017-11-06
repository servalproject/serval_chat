package org.servalproject.servalchat.navigation;

import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

/**
 * Created by jeremy on 6/11/17.
 */

public interface IRootContainer extends IContainerView{
	Toolbar getToolbar();
	CoordinatorLayout getCoordinator();
	void updateToolbar(boolean canGoBack);
	boolean onOptionsItemSelected(MenuItem item);
}
