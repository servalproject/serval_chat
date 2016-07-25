package org.servalproject.servalchat;

import android.os.Bundle;

import org.servalproject.servaldna.AbstractId;
import org.servalproject.servaldna.SubscriberId;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 19/07/16.
 */
public class NavHistory {
    public final SubscriberId identity;
    public final List<HistoryItem> history = new ArrayList<>();

    public HistoryItem getTop(){
        if (history.size()==0)
            history.add(new HistoryItem(identity == null ? Navigation.IdentityList : Navigation.Feed, null));
        return history.get(history.size()-1);
    }

    private NavHistory(SubscriberId identity){
        this.identity = identity;
    }

    public boolean up(){
        HistoryItem item = getTop();
        if (item.key.parent==null)
            return false;

        // always pop the current item
        pop();

        for(int i=history.size()-1;i>=0;i--){
            if (history.get(i).key == item.key.parent){
                // pop history back to here
                while(history.size()>i+1)
                    pop();
                return true;
            }
        }
        add(item.key.parent, null);
        return true;
    }

    private void pop(){
        history.remove(history.size() -1);
    }

    public boolean back(){
        if (history.size()<=1)
            return false;
        pop();
        return true;
    }

    public boolean add(HistoryItem item){
        HistoryItem top = getTop();
        // Ignore if we haven't gone anywhere
        if (top.equals(item))
            return false;
        history.add(item);
        return true;
    }

    public boolean add(Navigation key, Bundle args){
        return add(new HistoryItem(key, args));
    }

    public void save(Bundle state){
        if (identity!=null)
            state.putByteArray("history.identity", identity.getBinary());

        for (int i=0;i<history.size();i++){
            HistoryItem item = history.get(i);
            state.putString("history.key_"+i, item.key.name);
            if (item.args != null)
                state.putBundle("history.args_"+i, item.args);
        }
    }

    public static Bundle prepareNew (SubscriberId identity, Navigation key, Bundle args){
        Bundle state = new Bundle();
        if (identity!=null)
            state.putByteArray("history.identity", identity.getBinary());
        state.putString("history.key_0", key.name);
        if (args!=null && !args.isEmpty())
            state.putBundle("history.args_0", args);
        return state;
    }

    public static NavHistory restore(Bundle state){
        try {
            if (state == null)
                return new NavHistory(null);

            byte[] id = state.getByteArray("history.identity");
            SubscriberId identity = null;
            if (id!=null)
                identity = new SubscriberId(id);

            NavHistory ret = new NavHistory(identity);
            for(int i=0; ;i++){
                String key = state.getString("history.key_"+i);
                if (key == null)
                    break;
                Navigation nav = Navigation.NavMap.get(key);
                if (nav == null)
                    throw new IllegalStateException();
                Bundle args = state.getBundle("history.args_"+i);
                ret.add(nav, args);
            }
            return ret;
        } catch (AbstractId.InvalidBinaryException e) {
            throw new IllegalStateException(e);
        }
    }
}
