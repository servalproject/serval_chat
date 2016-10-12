package org.servalproject.servalchat.feeds;

import android.os.AsyncTask;

import org.servalproject.mid.Identity;
import org.servalproject.mid.IdentityFeed;
import org.servalproject.servalchat.views.Presenter;
import org.servalproject.servalchat.views.PresenterFactory;

/**
 * Created by jeremy on 8/08/16.
 */
public class MyFeedPresenter extends Presenter<MyFeed> {
    private final IdentityFeed feed;
    private final FeedAdapter adapter;

    protected MyFeedPresenter(PresenterFactory<MyFeed, ?> factory, String key, Identity identity) {
        super(factory, key, identity);
        this.feed = identity.getFeed();
        adapter = new FeedAdapter(feed);
    }

    public static final PresenterFactory<MyFeed, MyFeedPresenter> factory = new PresenterFactory<MyFeed, MyFeedPresenter>() {
        @Override
        protected MyFeedPresenter create(String key, Identity id) {
            return new MyFeedPresenter(this, key, id);
        }
    };

    private boolean posting = false;
    private void setEnabled(){
        MyFeed view = getView();
        if (view == null)
            return;
        view.post.setEnabled(!posting);
        view.message.setEnabled(!posting);
    }

    @Override
    protected void bind() {
        MyFeed view = getView();
        if (view == null)
            return;
        view.list.setAdapter(adapter);
        setEnabled();
    }

    public void post(){
        MyFeed view = getView();
        final String message = view.message.getText().toString();
        AsyncTask<Void, Void, Void> sendTask = new AsyncTask<Void, Void, Void>() {
            Exception e;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                posting = true;
                setEnabled();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                posting = false;
                MyFeed view = getView();
                if (view == null)
                    return;
                if (e == null){
                    view.message.setText("");
                }else{
                    view.activity.showError(e);
                }
                setEnabled();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    feed.sendMessage(message);
                }catch(Exception e){
                    this.e = e;
                }
                return null;
            }
        };
        sendTask.execute();
    }

    @Override
    public void onVisible() {
        super.onVisible();
        adapter.onVisible();
    }

    @Override
    public void onHidden() {
        super.onHidden();
        adapter.onHidden();
    }
}
