package org.servalproject.servalchat;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * Created by jeremy on 11/07/16.
 */
public abstract class SimpleRecyclerView<T, H extends RecyclerView.ViewHolder>
    extends RecyclerView{
    protected final ListAdapter listAdapter;

    public SimpleRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        listAdapter = new ListAdapter();
    }

    @Override
    protected void onAttachedToWindow() {
        setAdapter(listAdapter);
        super.onAttachedToWindow();
    }

    protected int getItemType(T item){
        return 0;
    }

    abstract protected H createHolder(ViewGroup parent, int viewType);

    abstract protected void bind(H holder, T item);

    protected long getId(T item){
        return -1;
    }

    abstract protected T get(int position);

    abstract protected int getCount();

    protected void notifyChanged(){
        listAdapter.notifyDataSetChanged();
    }

    public class ListAdapter extends RecyclerView.Adapter<H>{
        @Override
        public H onCreateViewHolder(ViewGroup parent, int viewType) {
            return createHolder(parent, viewType);
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

        @Override
        public int getItemViewType(int position) {
            return getItemType(get(position));
        }
    }

}
