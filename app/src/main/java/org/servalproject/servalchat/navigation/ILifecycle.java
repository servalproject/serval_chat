package org.servalproject.servalchat.navigation;

/**
 * Created by jeremy on 14/06/16.
 */
public interface ILifecycle {

    void onDetach(boolean configChange);

    void onVisible();

    void onHidden();
}
