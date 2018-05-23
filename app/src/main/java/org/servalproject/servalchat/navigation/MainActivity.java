package org.servalproject.servalchat.navigation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import org.servalproject.mid.Identity;
import org.servalproject.mid.ListObserver;
import org.servalproject.mid.Messaging;
import org.servalproject.mid.Observer;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Rhizome;
import org.servalproject.mid.Serval;
import org.servalproject.mid.networking.AbstractListObserver;
import org.servalproject.servalchat.App;
import org.servalproject.servalchat.CustomFileProvider;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.views.BackgroundWorker;
import org.servalproject.servalchat.views.DisplayError;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.meshmb.MeshMBCommon;

import java.io.File;
import java.util.Stack;

public class MainActivity extends AppCompatActivity implements IContainerView, MenuItem.OnMenuItemClickListener{

	private static final String TAG = "Activity";
	private App app;
	private IRootContainer rootContainer;
	private NavHistory history;
	private Serval serval;
	private Identity identity;
	private Peer peer;
	private InputMethodManager imm;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (App)getApplication();
		imm = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
		serval = Serval.getInstance();

		Intent intent = getIntent();
		init(intent, savedInstanceState);
	}

	private void init(Intent intent, Bundle savedInstanceState) {
		if ((savedInstanceState == null || savedInstanceState.isEmpty()) && intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null && extras.size() > 0) {
				// assume everything is fine...
				savedInstanceState = extras;
			}
		}

		history = NavHistory.restore(savedInstanceState);
		go();
		if (viewStack.isEmpty())
			throw new IllegalStateException();
	}

	private Stack<ViewState> viewStack = new Stack<>();

	private boolean isStarted = false;

	private final ListObserver<Identity> idLoaded = new AbstractListObserver<Identity>() {
		@Override
		public void added(Identity obj) {
			if (identity == null && obj.subscriber.equals(history.identity))
				go();
		}

		@Override
		public void reset() {
			go();
		}
	};

	private final Observer<Rhizome> rhizomeObserver = new Observer<Rhizome>() {
		@Override
		public void updated(Rhizome obj) {
			go();
		}
	};

	private void popViewsTo(int offset, boolean configChange) {
		for (int j = viewStack.size() - 1; j >= offset; j--) {
			ViewState v = viewStack.get(j);
			// remove views from their containers (if required)
			IContainerView container = j > 0 ? viewStack.get(j - 1).getContainer() : this;
			container.deactivate(v, configChange, isStarted);
			viewStack.remove(j);
		}
	}

	private void go() {
		HistoryItem item = history.getTop();
		Navigation n = item.key;
		Subscriber peerSubscriber = item.peer;
		Bundle args = item.args;
		peer = null;

		if (identity == null && history.identity != null)
			identity = serval.identities.getIdentity(history.identity);

		if (n.requiresId && identity == null && !serval.identities.isLoaded()) {
			// go() again if the identity appears
			serval.identities.listObservers.add(idLoaded);
			n = Navigation.Error;
			args = new Bundle();
			args.putInt(DisplayError.MESSAGE, R.string.startup);
			peerSubscriber = null;
		}

		if (n.requiresRhizome && !serval.rhizome.isEnabled()){
			n = Navigation.Error;
			args = new Bundle();
			args.putInt(DisplayError.MESSAGE, R.string.rhizome_disabled);
			peerSubscriber = null;
		}

		if (identity != null)
			serval.identities.listObservers.remove(idLoaded);

		// ignore the history for now, open the identity list so a pin can be provided
		// TODO not sure if this will work right...
		// Options;
		// 1) pin entry prompt
		// 2) hide the pin in the notification
		if (n.requiresId && identity == null) {
			n = Navigation.IdentityList;
			args = null;
			peerSubscriber = null;
		}

		if (peerSubscriber != null)
			peer = serval.knownPeers.getPeer(peerSubscriber);

		if (!n.children.isEmpty())
			throw new IllegalStateException();

		Stack<Navigation> newViews = new Stack<>();
		while (n != null) {
			newViews.push(n);
			n = n.containedIn;
		}
		// ignore common parent views
		n = newViews.pop();

		int i = 0;
		while (i < viewStack.size() && viewStack.get(i).key.equals(n)) {
			i++;
			n = newViews.empty() ? null : newViews.pop();
		}

		// pop un-common views
		popViewsTo(i, false);

		// add views (& locate containers?)
		ViewState parent = viewStack.isEmpty() ? null : viewStack.get(viewStack.size() - 1);
		while (n != null) {
			IContainerView container = (parent == null) ? this : parent.getContainer();
			if (container == null)
				throw new NullPointerException();
			parent = container.activate(n, identity, peer, args, isStarted);
			if (parent == null)
				throw new NullPointerException();
			viewStack.add(parent);
			if (newViews.empty())
				break;
			n = newViews.empty() ? null : newViews.pop();
		}

		rootContainer.updateToolbar(history.canGoBack());
		supportInvalidateOptionsMenu();

		View firstInput = null;
		for(ViewState state : viewStack){
			if ((firstInput = state.getTextInput())!=null)
				break;
		}
		if (firstInput == null)
			imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
	}

	public static Intent getIntentFor(Context context, Navigation key, Identity identity, Peer peer, Bundle args) {
		// Spawn new task
		Intent intent = new Intent();
		Class<?> activity = MainActivity.class;
		int flags = Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK;
		if (identity != null) {
			if (Build.VERSION.SDK_INT >= 21) {
				flags = Intent.FLAG_ACTIVITY_NEW_DOCUMENT
						| Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS;
				intent.setData(Uri.parse("org.servalproject.sid:" + identity.subscriber.sid.toHex()));
			} else {
				// TODO use & persist some kind of MRU ordering?
				switch ((int) identity.getId() % 4) {
					default:
						activity = Id4.class;
						break;
					case 1:
						activity = Id1.class;
						break;
					case 2:
						activity = Id2.class;
						break;
					case 3:
						activity = Id3.class;
						break;
				}
			}
		}
		intent.addFlags(flags);
		intent.setClass(context, activity);
		intent.putExtras(NavHistory.prepareNew(
				key,
				identity == null ? null : identity.subscriber.signingKey,
				peer == null ? null : peer.getSubscriber(),
				args));
		return intent;
	}

	public void go(Navigation key, Identity identity, Peer peer, Bundle args, boolean replace) {
		if (this.identity == identity) {
			go(key, peer, args, replace);
			return;
		}
		if (replace && history.back())
			go();
		startActivity(getIntentFor(this, key, identity, peer, args));
	}

	public void go(Navigation key) {
		go(key, false);
	}

	public void go(Navigation key, boolean replace) {
		go(key, null, null, replace);
	}

	public void go(Navigation key, Peer peer, Bundle args) {
		go(key, peer, args, false);
	}

	public void go(Navigation key, Peer peer, Bundle args, boolean replace) {
		HistoryItem item =new HistoryItem(key,
				identity == null ? null : this.identity.subscriber.signingKey,
				peer == null ? null : peer.getSubscriber(),
				args);
		go(item, replace);
	}

	public void go(HistoryItem item) {
		go(item, false);
	}

	public void go(HistoryItem item, boolean replace) {
		// record the change first, then create views
		if (history.add(item, replace))
			go();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		init(intent, null);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		history.save(outState);
	}

	public boolean goBack(){
		for(ViewState s :viewStack){
			if (s.visit(new ViewState.ViewVisitor() {
				@Override
				public boolean visit(View view) {
					return view instanceof IOnBack && ((IOnBack) view).onBack();
				}
			}))
				return true;
		}
		if (history.back()) {
			go();
			return true;
		}
		return false;
	}

	@Override
	public void onBackPressed() {
		if (!goBack())
			super.onBackPressed();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (rootContainer.onOptionsItemSelected(item))
			return true;
		return super.onOptionsItemSelected(item);
	}

	private static final int SHARE_APK = 1;

	private void shareApk(){
		if (App.isTesting()){
			showSnack("Ignoring firebase testlab", Snackbar.LENGTH_SHORT);
			return;
		}
		try {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_STREAM, CustomFileProvider.forPath(CustomFileProvider.APK_NAME));
			intent.setType("image/apk");
			intent.addCategory(Intent.CATEGORY_DEFAULT);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		} catch (Exception e) {
			showError(e);
		}
	}

	private void alterSubscription(final MeshMBCommon.SubscriptionAction action){
		final Peer alterPeer = peer;
		new BackgroundWorker(){
			@Override
			protected void onBackGround() throws Exception {
				identity.alterSubscription(action, alterPeer);
			}

			@Override
			protected void onComplete(Throwable t) {
				if (t != null)
					showError(t);
				else {
					int r=-1;
					switch (action){
						case Follow:
							r = R.string.followed;
							break;
						case Ignore:
							r = R.string.ignored;
							break;
						case Block:
							r = R.string.blocked;
							break;
					}
					showSnack(r, Snackbar.LENGTH_SHORT);
					supportInvalidateOptionsMenu();
				}
			}
		}.execute();
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
			case SHARE_APK:
				shareApk();
				return true;
			case FOLLOW:
				alterSubscription(MeshMBCommon.SubscriptionAction.Follow);
				return true;
			case IGNORE:
				alterSubscription(MeshMBCommon.SubscriptionAction.Ignore);
				return true;
			case BLOCK:
				alterSubscription(MeshMBCommon.SubscriptionAction.Block);
				return true;
		}
		return false;
	}

	private void populateMenu(Menu menu, View v) {
		if (v == null)
			return;
		if (v instanceof IHaveMenu) {
			((IHaveMenu) v).populateItems(menu);
		}
		if (v instanceof ViewGroup) {
			ViewGroup g = (ViewGroup) v;
			for (int i = 0; i < g.getChildCount(); i++)
				populateMenu(menu, g.getChildAt(i));
		}
	}

	private static final int FOLLOW = 2;
	private static final int IGNORE = 3;
	private static final int BLOCK = 4;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!App.isTesting()) {
			menu.add(Menu.NONE, SHARE_APK, Menu.NONE, R.string.share_app)
					.setOnMenuItemClickListener(this);
		}

		if (identity != null && peer != null && peer.getSubscriber().signingKey != null){
			Messaging.SubscriptionState state = identity.messaging.getSubscriptionState(peer.getSubscriber());

			if (state != null) {
				if (state != Messaging.SubscriptionState.Ignored) {
					MenuItem item = menu.add(Menu.NONE, IGNORE, Menu.NONE, R.string.ignore_feed)
							.setOnMenuItemClickListener(this)
							.setIcon(R.drawable.ic_remove_contact);
					MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
				}
				if (state != Messaging.SubscriptionState.Followed) {
					MenuItem item = menu.add(Menu.NONE, FOLLOW, Menu.NONE, R.string.follow_feed)
							.setOnMenuItemClickListener(this)
							.setIcon(R.drawable.ic_add_contact);
					MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
				}
				if (state != Messaging.SubscriptionState.Blocked) {
					MenuItem item = menu.add(Menu.NONE, BLOCK, Menu.NONE, R.string.block_contact)
							.setOnMenuItemClickListener(this)
							.setIcon(R.drawable.ic_block_contact);
					MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_NEVER);
				}
			}
		}
		populateMenu(menu, viewStack.peek().view);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	protected void onStop() {
		super.onStop();
		isStarted = false;
		for (ViewState state : viewStack) {
			ILifecycle lifecycle = state.getLifecycle();
			if (lifecycle != null)
				lifecycle.onHidden();
		}
	}

	@Override
	protected void onStart() {
		isStarted = true;
		for (ViewState state : viewStack) {
			ILifecycle lifecycle = state.getLifecycle();
			if (lifecycle != null)
				lifecycle.onVisible();
		}
		super.onStart();
		changingConfig = false;
	}

	private boolean changingConfig = false;

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		changingConfig = true;
		return super.onRetainCustomNonConfigurationInstance();
	}

	@Override
	protected void onDestroy() {
		if (Build.VERSION.SDK_INT >= 11)
			changingConfig = isChangingConfigurations();
		popViewsTo(0, changingConfig);
		super.onDestroy();
	}

	@Override
	public void deactivate(ViewState state, boolean configChange, boolean visible) {
		ILifecycle lifecycle = state.getLifecycle();
		if (visible && lifecycle != null)
			lifecycle.onHidden();
		if (lifecycle != null)
			lifecycle.onDetach(configChange);
		ViewGroup root = (ViewGroup)findViewById(android.R.id.content);
		root.removeView(state.view);
		serval.rhizome.observers.removeUI(rhizomeObserver);
	}

	@Override
	public ViewState activate(Navigation n, Identity identity, Peer peer, Bundle args, boolean visible) {
		ViewState ret = ViewState.Inflate(this, n, identity, peer, args);

		if (!ret.visit(new ViewState.ViewVisitor() {
			@Override
			public boolean visit(View view) {
				if (view instanceof IRootContainer)
					rootContainer = (IRootContainer) view;
				return rootContainer!=null;
			}
		}))
			throw new IllegalStateException();

		ret.view.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		setContentView(ret.view);

		serval.rhizome.observers.addUI(rhizomeObserver);

		ILifecycle lifecycle = ret.getLifecycle();
		if (visible && lifecycle != null)
			lifecycle.onVisible();
		return ret;
	}

	@NonNull
	@Override
	public ActionBar getSupportActionBar() {
		ActionBar ret = super.getSupportActionBar();
		if (ret == null)
			throw new IllegalStateException();
		return ret;
	}

	private class CrashReportException extends RuntimeException{
		CrashReportException(Throwable e) {
			super(e);
		}
	}

	public void showError(final Throwable e) {
		if (App.isTesting())
			throw new CrashReportException(e);
		showSnack(e.getMessage(), Snackbar.LENGTH_INDEFINITE, getString(R.string.email_log),
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						final Intent i = app.getErrorIntent(e);
						startActivity(i);
					}
				});
	}

	public void showSnack(CharSequence message, int length) {
		this.showSnack(message, length, null, null);
	}

	public void showSnack(int messageRes, int length) {
		this.showSnack(getString(messageRes), length, null, null);
	}

	public void showSnack(CharSequence message, int length, CharSequence actionLabel, View.OnClickListener action) {
		Snackbar s = Snackbar.make(rootContainer.getCoordinator(), message, length);
		if (action != null && actionLabel != null)
			s.setAction(actionLabel, action);
		s.show();
	}

}
