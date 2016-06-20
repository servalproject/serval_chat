package org.servalproject.servalchat;

/**
 * Created by jeremy on 15/06/16.
 */
public interface INavigatorHost extends IContainerView{
    void navigated(boolean backEnabled, boolean upEnabled);
    void rebuildMenu();
}
