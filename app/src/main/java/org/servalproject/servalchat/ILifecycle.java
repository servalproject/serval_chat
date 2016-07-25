package org.servalproject.servalchat;

/**
 * Created by jeremy on 14/06/16.
 */
public interface ILifecycle {

    void onDetach();

    void onVisible();

    void onHidden();
}
