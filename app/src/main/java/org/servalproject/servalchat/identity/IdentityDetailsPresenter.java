package org.servalproject.servalchat.identity;

import android.view.View;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.App;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.BackgroundWorker;
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
			view.update.setText(R.string.identity_update);
			view.sidLabel.setVisibility(View.VISIBLE);
			view.sid.setVisibility(View.VISIBLE);
			view.icon.setVisibility(View.VISIBLE);
		} else {
			view.sidLabel.setVisibility(View.GONE);
			view.sid.setVisibility(View.GONE);
			view.icon.setVisibility(View.GONE);
			view.update.setText(R.string.add_identity);
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

		updating = true;
		view.update.setEnabled(!updating);
		final String name = view.name.getText().toString();

		new BackgroundWorker() {
			private Identity result;

			@Override
			protected void onBackGround() throws Exception {
				Serval serval = Serval.getInstance();
				if (identity == null) {
					result = serval.identities.addIdentity("", name, "");
				} else {
					serval.identities.updateIdentity(
							identity, "", name, "");
					result = identity;
				}
			}

			@Override
			protected void onComplete(Throwable t) {
				updating = false;
				IdentityDetails view = getView();
				if (t != null) {
					if (view == null) {
						rethrow(t);
					} else {
						view.activity.showError(t);
					}
				} else if (view != null && view.activity != null){
					if (identity == null)
						view.activity.go(Navigation.MyFeed, result, null, null, true);
					else
						view.activity.go(Navigation.MyFeed);
				}
			}
		}.execute();
	}
}
