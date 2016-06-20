package org.servalproject.servalchat;

import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by jeremy on 20/06/16.
 */
public interface IHaveMenu {
    void populateItems(Menu menu);
    void onItem(MenuItem item);
}
