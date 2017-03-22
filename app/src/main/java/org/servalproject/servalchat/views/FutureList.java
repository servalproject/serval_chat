package org.servalproject.servalchat.views;

import android.support.v7.widget.RecyclerView;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Supposed to provide fast random access and grow efficiently from both ends
 * notifying an adapter when items are added.
 * Created by jeremy on 31/01/17.
 */

public class FutureList<T>
		extends AbstractList<T>{

	private ScrollingAdapter<T, ?> adapter;
	private final List<T> past = new ArrayList<>();
	private final List<T> future = new ArrayList<>();

	public FutureList(ScrollingAdapter<T, ?> adapter){
		this.adapter = adapter;
	}

	public T get(int position){
		if (position<0)
			return null;
		int futureSize = future.size();
		if (position>=0 && position < futureSize)
			return future.get(futureSize - 1 - position);
		position -= futureSize;
		if (position < past.size())
			return past.get(position);
		return null;
	}

	@Override
	public int size() {
		return past.size() + future.size();
	}

	@Override
	public void add(int index, T item){
		int futureSize = future.size();
		if (index <= futureSize){
			future.add(futureSize - index, item);
		}else{
			past.add(index - futureSize, item);
		}
		adapter.insertedItem(item, index);
	}

	@Override
	public boolean add(T item){
		past.add(item);
		adapter.insertedItem(item, size() - 1);
		return true;
	}

	public int find(T item){
		int index = Collections.binarySearch((List<? extends Comparable<? super T>>) this, item);
		if (index<0)
			index = (-index)-1;
		return index;
	}

	@Override
	public T remove(int index) {
		T ret = null;
		if (index>0){
			int futureSize = future.size();
			if (index>0 && index < futureSize) {
				ret = future.remove(futureSize - 1 - index);
			}else{
				index -= futureSize;
				if (index< past.size())
					ret = past.remove(index);
			}
			if (ret!=null)
				adapter.notifyItemRemoved(index);
		}
		return ret;
	}

	@Override
	public void clear() {
		past.clear();
		future.clear();
		adapter.notifyDataSetChanged();
	}
}
