package org.servalproject.servalchat;

import android.os.Bundle;
import android.view.View;

import org.servalproject.mid.Identity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jeremy on 20/07/16.
 */
public abstract class PresenterFactory<V extends View, P extends Presenter<V>> {

    private Map<String, P> presenters = new HashMap<>();

    public P getPresenter(V view, Identity id, Bundle savedState){
        String key = (id == null) ? "null" : id.subscriber.toString();
        P ret = presenters.get(key);
        if (ret == null) {
            ret = create(id);
            ret.restore(savedState);
        }
        ret.takeView(view);
        presenters.put(key, ret);
        return ret;
    }

    public void release(P presenter){
        Identity id = presenter.identity;
        String key = (id == null) ? "null" : id.subscriber.toString();
        presenters.remove(key);
    }

    protected abstract P create(Identity id);
}
