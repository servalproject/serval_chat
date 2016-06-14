package org.servalproject.mid;

import android.util.Log;

import org.servalproject.servaldna.SubscriberId;
import org.servalproject.servaldna.keyring.KeyringIdentity;
import org.servalproject.servaldna.keyring.KeyringIdentityList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jeremy on 6/06/16.
 */
public class Identities {
    private static final String TAG = "Identities";
    private final Serval serval;
    public final ListObserverSet<Identity> listObservers;
    private final List<Identity> identityList = new ArrayList<>();
    private final Map<SubscriberId, Identity> identities = new HashMap<>();
    private Identity selected;

    Identities(Serval serval){
        this.serval = serval;
        listObservers = new ListObserverSet<>(serval.uiHandler);
    }

    void onStart() {
        enterPin(null);
    }

    private void addId(KeyringIdentity id){
        Identity i = identities.get(id.sid);
        if (i==null){
            i = new Identity(serval.uiHandler, id.sid);
            identities.put(id.sid, i);
            identityList.add(i);
            if (selected==null)
                selected = i;
            i.update(id);
            listObservers.onAdd(i);
        }else{
            i.update(id);
            listObservers.onUpdate(i);
        }
    }

    public List<Identity> getIdentities(){
        return identityList;
    }

    public void selectIdentity(Identity id){
        if (id==null)
            throw new NullPointerException();
        if (id == selected)
            return;
        if (selected!=null)
            listObservers.onUpdate(selected);
        selected = id;
        listObservers.onUpdate(id);
    }

    public Identity getSelected(){
        return selected;
    }

    public void updateIdentity(final Identity id, final String did, final String name, final String pin){
        serval.runOnThreadPool(new Runnable() {
            @Override
            public void run() {
                try{
                    addId(serval.getResultClient().keyringSetDidName(id.sid, did, name, pin));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    public void addIdentity(final String did, final String name, final String pin){
        serval.runOnThreadPool(new Runnable() {
            @Override
            public void run() {
                try{
                    addId(serval.getResultClient().keyringAdd(did, name, pin));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    void enterPin(final String pin){
        serval.runOnThreadPool(new Runnable() {
            @Override
            public void run() {
                try {
                    KeyringIdentityList list = serval.getResultClient().keyringListIdentities(pin);
                    KeyringIdentity id=null;
                    while((id = list.nextIdentity())!=null)
                        addId(id);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }
}
