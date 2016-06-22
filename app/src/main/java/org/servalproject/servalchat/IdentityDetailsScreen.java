package org.servalproject.servalchat;

import android.os.AsyncTask;
import android.view.View;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Serval;

/**
 * Created by jeremy on 8/06/16.
 */
public class IdentityDetailsScreen extends Navigation implements View.OnClickListener {
    private Identity id;
    IdentityDetails view;
    private boolean updating = false;
    private final Navigator navigator;

    public IdentityDetailsScreen(Identity id) {
        super("IdentityDetails", R.string.identity_details, R.layout.identity_details, Navigation.Identity);
        this.id = id;
        navigator = Navigator.getNavigator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IdentityDetailsScreen that = (IdentityDetailsScreen) o;

        return id.equals(that.id);
    }

    public void bind(IdentityDetails view){
        this.view = view;
        view.name.setText(id == null ? null : id.getName());
        view.phone.setText(id == null ? null : id.getDid());
        view.update.setOnClickListener(this);
    }

    private AsyncTask<String, Void, Exception> updater = new AsyncTask<String, Void, Exception>(){
        @Override
        protected void onPostExecute(Exception e) {
            updating = false;
            boolean viewCurrent = (view!=null && navigator.isCurrentView(IdentityDetailsScreen.this));

            if (viewCurrent)
                view.update.setEnabled(!updating);

            if (e != null)
                navigator.showError(e);
            else if (viewCurrent)
                navigator.gotoView(IdentityDetailsScreen.this.parent);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            updating = true;
            if (view!=null && navigator.isCurrentView(IdentityDetailsScreen.this))
                view.update.setEnabled(!updating);
        }

        @Override
        protected Exception doInBackground(String... params) {
            try {
                Serval serval = Serval.getInstance();
                if (serval == null)
                    return null;

                if (id == null){
                    id = serval.identities.addIdentity(params[0], params[1], params[2]);
                }else {
                    serval.identities.updateIdentity(
                            id, params[0], params[1], params[2]);
                }
            } catch (Exception e) {
                return e;
            }
            return null;
        }
    };

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }

    @Override
    public void onClick(View v) {
        if (updating)
            return;
        updater.execute(view.phone.getText().toString(), view.name.getText().toString(), "");
    }
}
