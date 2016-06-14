package org.servalproject.servalchat;

import android.view.LayoutInflater;

/**
 * Created by jeremy on 14/06/16.
 */
public interface IContainerView {
    void removeView(Navigation n);
    IContainerView addView(LayoutInflater inflater, Navigation n);
}
