package org.servalproject.servalchat.identity;

import android.os.AsyncTask;
import android.view.View;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.App;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.Identicon;
import org.servalproject.servalchat.views.Presenter;
import org.servalproject.servalchat.views.PresenterFactory;

/**
 * Created by jeremy on 20/07/16.
 */
public class IdentityDetailsPresenter extends Presenter<IdentityDetails> {

	public static PresenterFactory<IdentityDetails, IdentityDetailsPresenter> factory
			= new PresenterFactory<IdentityDetails, IdentityDetailsPresenter>() {
		@Override
		protected IdentityDetailsPresenter create(String key, Identity id, Peer peer) {
			return new IdentityDetailsPresenter(this, key, id);
		}
	};

	private boolean updating = false;

	private IdentityDetailsPresenter(PresenterFactory<IdentityDetails, ?> factory, String key, Identity id) {
		super(factory, key, id);
	}

	@Override
	protected void bind(IdentityDetails view) {
		if (identity != null) {
			view.name.setText(identity.getName());
			view.sid.setText(identity.subscriber.sid.toHex());
			view.icon.setImageDrawable(identity.getIcon());
			view.sidLabel.setVisibility(View.VISIBLE);
			view.sid.setVisibility(View.VISIBLE);
			view.icon.setVisibility(View.VISIBLE);
		} else {
			view.sidLabel.setVisibility(View.GONE);
			view.sid.setVisibility(View.GONE);
			view.icon.setVisibility(View.GONE);
			if (App.isTesting())
				view.name.setText("Test User");
		}
		view.update.setEnabled(!updating);
	}

	@Override
	public void onVisible() {
		IdentityDetails view = getView();
		if (view != null)
			bind(view);
	}

	public void update() {
		if (updating)
			return;

		IdentityDetails view = getView();
		if (view == null)
			return;

		AsyncTask<String, Void, Exception> updater = new AsyncTask<String, Void, Exception>() {
			private Identity result;

			@Override
			protected void onPostExecute(Exception e) {
				updating = false;
				IdentityDetails view = getView();

				if (view != null) {
					view.update.setEnabled(!updating);
					if (e != null)
						view.activity.showError(e);
					else {
						view.activity.go(Navigation.MyFeed, result, null, null);
					}
				}
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				updating = true;
				IdentityDetails view = getView();
				if (view != null)
					view.update.setEnabled(!updating);
			}

			@Override
			protected Exception doInBackground(String... params) {
				try {
					Serval serval = Serval.getInstance();
					if (serval == null)
						return null;

					if (identity == null) {
						result = serval.identities.addIdentity(params[0], params[1], params[2]);
					} else {
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

		updater.execute("", view.name.getText().toString(), "");
	}
}
