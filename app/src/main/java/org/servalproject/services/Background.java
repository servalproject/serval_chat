package org.servalproject.services;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Created by jeremy on 20/01/16.
 */
public class Background {
    private Handler backgroundHandler = null;
    public Background(String name){
        HandlerThread backgroundThread = new HandlerThread("Background "+name);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    // separate threads for different priorities?
    public static final Background Low = new Background("Low");

    public void run(Runnable r){
        run(r, 0);
    }
    public void run(Runnable r, int delay){
        backgroundHandler.removeCallbacks(r);
        backgroundHandler.postDelayed(r, delay);
    }
}
