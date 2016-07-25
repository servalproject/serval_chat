package org.servalproject.servalchat;

import android.os.Bundle;

/**
 * Created by jeremy on 20/07/16.
 */
public class HistoryItem {
    public final Navigation key;
    public final Bundle args;

    public HistoryItem(Navigation key, Bundle args) {
        this.key = key;
        this.args = args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HistoryItem that = (HistoryItem) o;

        // TODO compare arg values?
        return key.equals(that.key) && args == that.args;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
