package org.servalproject.servalchat;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;

import org.servalproject.mid.ListObserver;
import org.servalproject.mid.ListObserverSet;

/**
 * Created by jeremy on 20/06/16.
 */
public abstract class ObservedRecyclerView<T, H extends RecyclerView.ViewHolder>
        extends RecyclerView
        implements ListObserver<T>, IActivityLifecycle{

    private final ListObserverSet<T> observerSet;
    private int generation =-1;
    protected final ListAdapter listAdapter;

    public ObservedRecyclerView(ListObserverSet<T> observerSet, Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.observerSet = observerSet;
        listAdapter = new ListAdapter();
    }

    @Override
    protected void onAttachedToWindow() {
        setAdapter(listAdapter);
        super.onAttachedToWindow();
    }

    abstract protected H createHolder(ViewGroup parent);

    abstract protected void bind(H holder, T item);

    protected long getId(T item){
        return -1;
    }

    abstract protected T get(int position);

    abstract protected int getCount();

    @Override
    public void onStart() {
        int g = observerSet.add(this);
        if (g!=generation)
            listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        generation = observerSet.remove(this);
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void added(T obj) {
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void removed(T obj) {
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void updated(T obj) {
        listAdapter.notifyDataSetChanged();
    }

    protected class ListAdapter extends RecyclerView.Adapter<H>{

        @Override
        public H onCreateViewHolder(ViewGroup parent, int viewType) {
            return createHolder(parent);
        }

        @Override
        public void onBindViewHolder(H holder, int position) {
            bind(holder, get(position));
        }

        @Override
        public int getItemCount() {
            return getCount();
        }

        public long getItemId(int position) {
            return getId(get(position));
        }
    }
}
