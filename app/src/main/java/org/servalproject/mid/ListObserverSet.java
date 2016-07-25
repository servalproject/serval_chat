package org.servalproject.mid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by jeremy on 11/05/16.
 */
public class ListObserverSet<T> implements UIHandler.MessageHandler<T> {
    private final Set<ListObserver<T>> observers = new HashSet<>();

    private static final int ADD=1;
    private static final int REMOVE=2;
    private static final int UPDATE=3;
    private static final int RESET=4;
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

    public boolean hasObservers(){
        return !observers.isEmpty();
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
    void onReset(){
        generation++;
        if (observers.isEmpty())
            return;
        uiHandler.sendMessage(this, null, RESET);
    }

    @Override
    public void handleMessage(T obj, int what) {
        if (observers.isEmpty())
            return;
        // clone the list so we can remove while iterating
        List<ListObserver<T>> notify = new ArrayList<>(observers);
        for(ListObserver<T> observer:notify){
            switch (what){
                case ADD: observer.added(obj); break;
                case REMOVE: observer.removed(obj); break;
                case UPDATE: observer.updated(obj); break;
                case RESET: observer.reset(); break;
            }
        }
    }
}
