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
    private int lastVisible=0;
    private int minPrefetch=5;
    private boolean fetching=false;
    private boolean atEnd = false;
    private static final String TAG = "InfiniteView";

    public InfiniteRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        layoutManager = new LinearLayoutManager(context);
        setLayoutManager(layoutManager);

        this.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
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
        lastVisible = layoutManager.findLastVisibleItemPosition();
        if (!atEnd && lastVisible + minPrefetch > getCount()){
            fetching = true;
            fetchMore();
        }else{
            fetching = false;
        }
    }

    protected void begin(){
        if (!fetching && getCount()==0){
            fetching = true;
            fetchMore();
        }
    }
    // must call fetchComplete from within the ui thread once batch of items have been added.
    protected abstract void fetchMore();

    @Override
    protected T get(int position) {
        int futureSize = future.size();
        if (position < futureSize)
            return future.get(futureSize - 1 - position);
        return past.get(position - futureSize);
    }

    @Override
    protected int getCount() {
        int ret = past.size() + future.size();
        return ret;
    }

    // TODO scrolling is bound to be broken in some fashion when future items are added.

    public void addFuture(T item){
        int size = getCount();
        future.add(item);
        if (size==0)
            listAdapter.notifyDataSetChanged();
        else
            listAdapter.notifyItemInserted(0);
    }

    // newest items should be at the end of the collection
    public void addFuture(Collection<T> items){
        int size = getCount();
        future.addAll(items);
        if (size==0)
            listAdapter.notifyDataSetChanged();
        else
            listAdapter.notifyItemRangeInserted(0, items.size());
    }

    public void addPast(T item){
        int size = getCount();
        past.add(item);
        if (size==0)
            listAdapter.notifyDataSetChanged();
        else
            listAdapter.notifyItemInserted(size);
    }

    // oldest items should be at the end of the collection
    public void addPast(Collection<T> items){

        int count = items.size();
        if (count==0)
            return;
        int size = getCount();

        past.addAll(items);
        if (size==0)
            listAdapter.notifyDataSetChanged();
        else
            listAdapter.notifyItemRangeInserted(size, count);
    }
}
