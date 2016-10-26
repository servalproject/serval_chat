package org.servalproject.servalchat.navigation;

import android.os.Bundle;

import org.servalproject.mid.Identity;

/**
 * Created by jeremy on 14/06/16.
 */
public interface IContainerView {
	void deactivate(ViewState state, boolean configChange);

	ViewState activate(Navigation n, Identity identity, Bundle args);
}
