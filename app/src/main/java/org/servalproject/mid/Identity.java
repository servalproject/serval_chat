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
    private KeyringIdentity identity;

    private static long nextId=0;
    private final long id;

    public Identity(Handler handler, SubscriberId sid){
        this.sid = sid;
        observers = new ObserverSet<>(handler, this);
        id = nextId++;
    }

    public void update(KeyringIdentity id){
        this.identity = id;
        observers.onUpdate();
    }

    public String getName(Context c) {
        return identity==null || identity.name==null ? c.getString(R.string.no_name) : identity.name;
    }

    public String getDid(Context c) {
        return identity==null || identity.did == null ? c.getString(R.string.no_number) : identity.did;
    }

    public long getId() {
        return id;
    }
}
