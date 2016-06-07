package org.servalproject.mid;

import android.content.Context;
import android.os.Handler;

import org.servalproject.servalchat.R;
import org.servalproject.servaldna.SubscriberId;
import org.servalproject.servaldna.keyring.KeyringIdentity;

/**
 * Created by jeremy on 6/06/16.
 */
public class Identity {
    public final SubscriberId sid;
    public final ObserverSet<Identity> observers;
    private KeyringIdentity id;

    public Identity(Handler handler, SubscriberId sid){
        this.sid = sid;
        observers = new ObserverSet<>(handler, this);
    }

    public void update(KeyringIdentity id){
        this.id = id;
        observers.onUpdate();
    }

    public String getName(Context c) {
        return id==null || id.name==null ? c.getString(R.string.no_name) : id.name;
    }

    public String getDid(Context c) {
        return id==null || id.did == null ? c.getString(R.string.no_number) : id.did;
    }
}
