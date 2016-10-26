package org.servalproject.servalchat.navigation;

import android.view.View;

/**
 * Created by jeremy on 15/06/16.
 */
public interface INavigatorHost extends IContainerView {
	void navigated(boolean backEnabled, boolean upEnabled);

	void rebuildMenu();

	void showSnack(CharSequence message, int length, CharSequence actionLabel, View.OnClickListener action);
}
