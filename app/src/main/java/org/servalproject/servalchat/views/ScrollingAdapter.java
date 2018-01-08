package org.servalproject.servalchat.views;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.servalproject.mid.IObservableList;
import org.servalproject.mid.ListObserver;
import org.servalproject.servalchat.navigation.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 8/08/16.
 */
public abstract class ScrollingAdapter<T, VH extends BasicViewHolder>
		extends RecyclerView.Adapter<BasicViewHolder> implements ListObserver<T> {
	private LinearLayoutManager layoutManager;
	private IObservableList<T, ?> list;
	protected final FutureList<T> items = new FutureList<>(this);
	private boolean hasMore = true;
	private int fetchCount;
	private static final int SPINNER = 0;

	public ScrollingAdapter(IObservableList<T, ?> list) {
		this.list = list;
		if (list == null)
			hasMore = false;
	}

	protected abstract void bind(VH holder, T item);
	protected abstract VH create(ViewGroup parent, int viewType);

	protected void bindItem(VH holder, int position){
		bind(holder, getItem(position));
	}

	@Override
	public void onBindViewHolder(BasicViewHolder holder, int position) {
		if (holder instanceof SpinnerViewHolder)
			return; // nothing to bind
		bindItem((VH)holder, position);
	}

	@Override
	public BasicViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if (viewType==SPINNER)
			return new SpinnerViewHolder(parent);
		return create(parent, viewType - 1);
	}

	// override to track where it was inserted
	public void insertedItem(T item, int position){
		notifyItemInserted(position);
		if (layoutManager != null && position == 0)
			layoutManager.scrollToPosition(0);
	}

	private void addPast(T item){
		addItem(items.size(), item);
	}

	private void addFuture(T item){
		int index=0;
		// reading future items, usually returns a burst of items in reverse order.
		if (item instanceof Comparable<?>)
			index = items.find(item);
		addItem(index, item);
	}

	// override to filter items out
	protected void addItem(int index, T item) {
		items.add(index, item);
	}

	@Override
	public int getItemCount() {
		int count = items.size();
		if (hasMore)
			count++;
		return count;
	}

	protected int getItemType(T item){
		return 0;
	};

	protected T getItem(int position) {
		if (position < 0)
			return null;
		if (position < items.size())
			return items.get(position);
		return null;
	}

	@Override
	public int getItemViewType(int position) {
		T item = getItem(position);
		if (item == null)
			return SPINNER;
		return getItemType(getItem(position)) + 1;
	}

	private BackgroundWorker fetcher = new BackgroundWorker(){
		List<T> results = new ArrayList<T>();
		@Override
		protected void onBackGround() throws Exception {
			for (int i = 0; i < fetchCount; i++) {
				T msg;
				results.add(msg = list.next());
				if (msg == null)
					break;
			}
		}

		@Override
		protected void onComplete(Throwable t) {
			for (T msg : results){
				if (msg == null) {
					hasMore = false;
					notifyItemRemoved(items.size());
				} else {
					addPast(msg);
				}
			}
			results.clear();
			if (t!=null){
				MainActivity activity = getActivity();
				if (activity!=null){
					activity.showError(t);
				}else{
					if (t instanceof RuntimeException)
						throw (RuntimeException)t;
					throw new IllegalStateException(t);
				}
				if (hasMore){
					hasMore = false;
					notifyItemRemoved(items.size());
				}
			}
			testPosition();
		}
	};

	protected abstract MainActivity getActivity();

	private void testPosition() {
		if (fetcher.isRunning() || !hasMore || layoutManager == null)
			return;

		int lastVisible = layoutManager.findLastVisibleItemPosition();
		fetchCount = lastVisible + 15 - (items.size());

		if (fetchCount <= 0)
			return;

		fetcher.execute();
	}

	@Override
	public void onAttachedToRecyclerView(RecyclerView recyclerView) {
		super.onAttachedToRecyclerView(recyclerView);
		layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				testPosition();
			}
		});
	}

	@Override
	public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
		super.onDetachedFromRecyclerView(recyclerView);
		layoutManager = null;
	}

	public void onVisible() {
		if (list == null)
			return;
		list.observe(this);
		testPosition();
	}

	public void onHidden() {
		if (list == null)
			return;
		list.stopObserving(this);
	}

	@Override
	public void added(T obj) {
		addFuture(obj);
	}

	@Override
	public void removed(T obj) {

	}

	@Override
	public void updated(T obj) {

	}

	@Override
	public void reset() {

	}

	public void clear() {
		items.clear();
		if (list != null)
			list.close();
	}
}
