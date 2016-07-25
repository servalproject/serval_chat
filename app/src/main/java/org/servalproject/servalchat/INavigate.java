package org.servalproject.servalchat;

import android.os.Bundle;

import org.servalproject.mid.Identity;

/**
 * Created by jeremy on 14/06/16.
 */
public interface INavigate {
    ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args);
}
