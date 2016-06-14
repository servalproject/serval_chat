package org.servalproject.mid;

import android.os.Handler;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by jeremy on 11/05/16.
 */
public class ListObserverSet<T> implements UIHandler.MessageHandler<T> {
    private final Set<ListObserver<T>> observers = new HashSet<>();

    private static final int ADD=1;
    private static final int REMOVE=2;
    private static final int UPDATE=3;
    private int generation=0;

    private final UIHandler uiHandler;
    ListObserverSet(UIHandler uiHandler){
        this.uiHandler = uiHandler;
    }

    public int add(ListObserver<T> observer){
        observers.add(observer);
        return generation;
    }

    public int remove(ListObserver<T> observer){
        observers.remove(observer);
        return generation;
    }

    void onAdd(T t){
        generation++;
        if (observers.isEmpty())
            return;
        uiHandler.sendMessage(this, t, ADD);
    }
    void onRemove(T t){
        generation++;
        if (observers.isEmpty())
            return;
        uiHandler.sendMessage(this, t, REMOVE);
    }
    void onUpdate(T t){
        generation++;
        if (observers.isEmpty())
            return;
        uiHandler.sendMessage(this, t, UPDATE);
    }

    @Override
    public void handleMessage(T obj, int what) {
        if (observers.isEmpty())
            return;
        for(ListObserver<T> observer:observers){
            switch (what){
                case ADD: observer.added(obj); break;
                case REMOVE: observer.removed(obj); break;
                case UPDATE: observer.updated(obj); break;
            }
        }
    }
}
