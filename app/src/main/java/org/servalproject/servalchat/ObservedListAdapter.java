package org.servalproject.servalchat;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.servalproject.mid.ListObserver;
import org.servalproject.mid.ListObserverSet;

import java.util.List;

/**
 * Created by jeremy on 8/06/16.
 */
public class ObservedListAdapter<T, H extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<H>
        implements ListObserver<T>, IActivityLifecycle {

    public interface Binder<T, H>{
        H createHolder(ViewGroup parent);
        void bind(H holder, T item);
        long getId(T item);
    }

    private final ListObserverSet<T> observerSet;
    protected final List<T> items;
    private final Binder<T, H> binder;
    private int generation =-1;

    public ObservedListAdapter(ListObserverSet<T> observerSet, Binder<T, H> binder, List<T> items){
        this.observerSet = observerSet;
        this.items = items;
        this.binder = binder;
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onStart() {
        int g = observerSet.add(this);
        if (g!=generation)
            notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        generation = observerSet.remove(this);
    }

    @Override
    public void onPause(){
    }

    @Override
    public H onCreateViewHolder(ViewGroup parent, int viewType) {
        return binder.createHolder(parent);
    }

    @Override
    public void onBindViewHolder(H holder, int position) {
        binder.bind(holder, get(position));
    }

    protected T get(int position){
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return binder.getId(get(position));
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public void added(T obj) {
        notifyDataSetChanged();
    }

    @Override
    public void removed(T obj) {
        notifyDataSetChanged();
    }

    @Override
    public void updated(T obj) {
        notifyDataSetChanged();
    }
}
