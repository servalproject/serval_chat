package org.servalproject.servalchat;

/**
 * Created by jeremy on 14/06/16.
 */
public interface IActivityLifecycle {
    void onStart();

    void onStop();

    void onPause();

    void onResume();
}
