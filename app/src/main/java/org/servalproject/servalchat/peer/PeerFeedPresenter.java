package org.servalproject.servalchat.peer;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.servalproject.mid.Identity;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.views.Presenter;
import org.servalproject.servalchat.views.PresenterFactory;
import org.servalproject.servaldna.AbstractId;
import org.servalproject.servaldna.SubscriberId;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 3/08/16.
 */
public class PeerFeedPresenter extends Presenter<PeerFeed> {

    private List<Post> posts = new ArrayList<>();
    private RecyclerView.Adapter<PostHolder> adapter;

    protected PeerFeedPresenter(PresenterFactory<PeerFeed, ?> factory, String key, Identity identity) {
        super(factory, key, identity);
        posts.add(new Post());
    }

    public static PresenterFactory<PeerFeed, PeerFeedPresenter> factory
            = new PresenterFactory<PeerFeed, PeerFeedPresenter>() {

        @Override
        protected String getKey(Identity id, Bundle savedState) {
            try {
                SubscriberId them = new SubscriberId(savedState.getByteArray("them"));
                return id.subscriber.sid.toHex()+":"+them.toHex();
            } catch (AbstractId.InvalidBinaryException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        protected PeerFeedPresenter create(String key, Identity id) {
            return new PeerFeedPresenter(this, key, id);
        }

    };

    @Override
    protected void bind() {
        PeerFeed feed = getView();

        feed.list.setAdapter(adapter);
    }

    @Override
    protected void restore(Bundle config) {
        adapter = new RecyclerView.Adapter<PostHolder>() {
            @Override
            public PostHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                return new PostHolder(inflater.inflate(R.layout.placeholder, parent, false));
            }

            @Override
            public void onBindViewHolder(PostHolder holder, int position) {

            }

            @Override
            public int getItemCount() {
                return posts.size();
            }
        };
    }

    public class Post{

    }

    public class PostHolder extends RecyclerView.ViewHolder{
        public PostHolder(View itemView) {
            super(itemView);
        }
    }
}
