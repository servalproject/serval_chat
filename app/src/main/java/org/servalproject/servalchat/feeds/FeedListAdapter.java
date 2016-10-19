package org.servalproject.servalchat.feeds;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.FeedList;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.views.ScrollingAdapter;
import org.servalproject.servaldna.BundleId;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.rhizome.RhizomeListBundle;

import java.util.HashSet;

/**
 * Created by jeremy on 11/10/16.
 */
public class FeedListAdapter extends ScrollingAdapter<RhizomeListBundle, FeedListAdapter.FeedHolder> {

    private final PublicFeedsPresenter presenter;
    private static final int SPINNER = 0;
    private static final int FEED = 1;
    private HashSet<BundleId> bundles = new HashSet<>();

    public FeedListAdapter(FeedList list, PublicFeedsPresenter presenter) {
        super(list);
        this.presenter = presenter;
    }

    private void removeItem(BundleId id){
        // find the old item and remove it.
        for (int i=0;i<future.size();i++){
            if (future.get(i).manifest.id.equals(id)){
                future.remove(i);
                notifyItemRemoved(future.size() - i);
                return;
            }
        }
        for (int i=0;i<past.size();i++){
            if (past.get(i).manifest.id.equals(id)){
                past.remove(i);
                notifyItemRemoved(future.size() + i);
                return;
            }
        }
    }

    @Override
    protected void addItem(RhizomeListBundle item, boolean inPast) {
        if (bundles.contains(item.manifest.id)){
            // the back end could write the details of an old bundle
            // then we could hear about an updated version,
            // then scroll back and see the details of the old version
            if (inPast)
                return;
            removeItem(item.manifest.id);
        }else{
            bundles.add(item.manifest.id);
        }
        super.addItem(item, inPast);
    }

    @Override
    protected void bind(FeedHolder holder, RhizomeListBundle item) {
        holder.bind(item);
    }

    @Override
    protected int getItemType(RhizomeListBundle item) {
        return (item == null) ? SPINNER : FEED;
    }

    @Override
    public FeedHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType){
            case 0:
                return new SpinnerHolder(inflater.inflate(R.layout.progress, parent, false));
            default:
                return new TextHolder(inflater.inflate(R.layout.feed, parent, false));
        }
    }

    public abstract class FeedHolder extends RecyclerView.ViewHolder{
        public FeedHolder(View itemView) {
            super(itemView);
        }
        public void bind(RhizomeListBundle item){}
    }

    public class SpinnerHolder extends FeedHolder{
        public SpinnerHolder(View itemView) {
            super(itemView);
        }
    }

    public class TextHolder extends FeedHolder implements View.OnClickListener{
        private TextView name;
        private Subscriber subscriber;

        public TextHolder(View itemView) {
            super(itemView);
            this.name = (TextView)this.itemView.findViewById(R.id.name);
            this.itemView.setOnClickListener(this);
        }

        public void bind(RhizomeListBundle item){
            subscriber = new Subscriber(
                    item.author!=null ? item.author : item.manifest.sender,
                    item.manifest.id, true);
            if (item.manifest.name==null || "".equals(item.manifest.name))
                name.setText(subscriber.sid.abbreviation());
            else
                name.setText(item.manifest.name);
        }

        @Override
        public void onClick(View v) {
            presenter.openFeed(subscriber);
        }
    }
}
