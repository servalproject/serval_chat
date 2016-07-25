package org.servalproject.servalchat;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by jeremy on 11/07/16.
 */
public abstract class InfiniteRecyclerView<T, H extends RecyclerView.ViewHolder>
    extends SimpleRecyclerView<T, H> {
    private final List<T> past = new ArrayList<>();
    private final List<T> future = new ArrayList<>();
    private final LinearLayoutManager layoutManager;
    private int firstVisible=0;
    private int lastVisible=0;
    private int minPrefetch=5;
    private boolean fetching=false;
    private boolean atEnd = false;

    public InfiniteRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        layoutManager = new LinearLayoutManager(context);
        setLayoutManager(layoutManager);

        this.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                firstVisible = layoutManager.findFirstVisibleItemPosition();
                lastVisible = layoutManager.findLastVisibleItemPosition();
                if (!atEnd && !fetching && lastVisible + minPrefetch > getCount()){
                    fetching = true;
                    fetchMore();
                }
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    protected void fetchComplete(boolean atEnd){
        this.atEnd = atEnd;
        if (!atEnd && lastVisible + minPrefetch > getCount()){
            fetching = true;
            fetchMore();
        }else{
            fetching = false;
        }
    }

    // must call fetchComplete from within the ui thread once batch of items have been added.
    protected abstract void fetchMore();

    @Override
    protected T get(int position) {
        int futureSize = future.size();
        if (position < futureSize)
            return future.get(futureSize - position);
        return past.get(position - futureSize);
    }

    @Override
    protected int getCount() {
        return past.size() + future.size();
    }

    // TODO scrolling is bound to be broken in some fashion when future items are added.

    public void addFuture(T item){
        future.add(item);
        listAdapter.notifyItemInserted(0);
    }

    // newest items should be at the end of the collection
    public void addFuture(Collection<T> items){
        future.addAll(items);
        listAdapter.notifyItemRangeInserted(0, items.size());
    }

    public void addPast(T item){
        past.add(item);
        listAdapter.notifyItemInserted(getCount() -1);
    }

    // oldest items should be at the end of the collection
    public void addPast(Collection<T> items){
        int count = items.size();
        past.addAll(items);
        listAdapter.notifyItemRangeInserted(getCount() - count, count);
    }
}
