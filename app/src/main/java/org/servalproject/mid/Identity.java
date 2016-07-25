package org.servalproject.mid;

import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.keyring.KeyringIdentity;

/**
 * Created by jeremy on 6/06/16.
 */
public class Identity {
    public final Subscriber subscriber;
    public final ObserverSet<Identity> observers;
    private KeyringIdentity identity;

    private static long nextId=0;
    private final long id;
    public final Messaging messaging;

    public Identity(Serval serval, Subscriber subscriber){
        this.subscriber = subscriber;
        id = nextId++;
        if (serval == null){
            // dummy object for ui design
            messaging = null;
            observers = null;
        }else{
            observers = new ObserverSet<>(serval.uiHandler, this);
            this.messaging = new Messaging(serval, this);
        }
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
