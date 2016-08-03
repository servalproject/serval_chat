package org.servalproject.servalchat;

import android.app.Application;
import android.content.Intent;

import org.servalproject.mid.Serval;
import org.servalproject.servalchat.navigation.Navigation;

/**
 * Created by jeremy on 11/05/16.
 */
public class App extends Application {

    public static Navigation toNavigation(Intent intent){
        if (intent == null)
            return null;
        // TODO
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // always start our daemon thread
        Serval serval = Serval.start(this);
        Notifications.onStart(serval, this);
    }
}
