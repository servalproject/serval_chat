package org.servalproject.servalchat;

import android.os.AsyncTask;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Serval;

/**
 * Created by jeremy on 20/07/16.
 */
public class IdentityDetailsPresenter extends Presenter<IdentityDetails> {

    public static PresenterFactory<IdentityDetails, IdentityDetailsPresenter> factory
            = new PresenterFactory<IdentityDetails, IdentityDetailsPresenter>() {
        @Override
        protected IdentityDetailsPresenter create(String key, Identity id) {
            return new IdentityDetailsPresenter(this, key, id);
        }
    };

    private boolean updating = false;

    private IdentityDetailsPresenter(PresenterFactory<IdentityDetails, ?> factory, String key, Identity id) {
        super(factory, key, id);
    }

    @Override
    protected void bind() {
        getView().name.setText(identity == null ? null : identity.getName());
        getView().phone.setText(identity == null ? null : identity.getDid());
        getView().update.setEnabled(!updating);
    }

    @Override
    public void onVisible() {
        bind();
    }

    public void update() {
        if (updating)
            return;

        AsyncTask<String, Void, Exception> updater = new AsyncTask<String, Void, Exception>(){
            private Identity result;
            @Override
            protected void onPostExecute(Exception e) {
                updating = false;
                IdentityDetails view = getView();

                if (view != null) {
                    view.update.setEnabled(!updating);
                    if (e!=null)
                        view.activity.showError(e);
                    else {
                        view.activity.go(result, Navigation.Feed, null);
                    }
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                updating = true;
                IdentityDetails view = getView();
                if (view!=null)
                    view.update.setEnabled(!updating);
            }

            @Override
            protected Exception doInBackground(String... params) {
                try {
                    Serval serval = Serval.getInstance();
                    if (serval == null)
                        return null;

                    if (identity == null){
                        result = serval.identities.addIdentity(params[0], params[1], params[2]);
                    }else {
                        result = identity;
                        serval.identities.updateIdentity(
                                identity, params[0], params[1], params[2]);
                    }
                } catch (Exception e) {
                    return e;
                }
                return null;
            }
        };

        IdentityDetails view = getView();
        updater.execute(view.phone.getText().toString(), view.name.getText().toString(), "");
    }
}
