package org.servalproject.servalchat.navigation;

import android.os.Bundle;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Peer;

/**
 * Created by jeremy on 14/06/16.
 */
public interface INavigate {
	ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args);
}
