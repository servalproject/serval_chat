package org.servalproject.mid;

import android.os.Handler;

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
        observers = (handler == null) ? null : new ObserverSet<>(handler, this);
        id = nextId++;
    }

    public void update(KeyringIdentity id){
        this.identity = id;
        if (observers != null)
            observers.onUpdate();
    }

    public String getName() {
        return identity==null ? null : identity.name;
    }

    public String getDid() {
        return identity==null ? null : identity.did;
    }

    public long getId() {
        return id;
    }
}
