package org.servalproject.servalchat.navigation;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.servalproject.mid.Identity;
import org.servalproject.mid.ListObserver;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;

import java.io.File;
import java.util.Stack;

public class MainActivity extends AppCompatActivity implements IContainerView, MenuItem.OnMenuItemClickListener {

	private static final String TAG = "Activity";
	private LinearLayout rootLayout;
	private CoordinatorLayout coordinator;
	private NavHistory history;
	private Serval serval;
	private Identity identity;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		serval = Serval.getInstance();
		setContentView(R.layout.activity);
		rootLayout = (LinearLayout) findViewById(R.id.root_layout);
		Toolbar toolbar = (Toolbar) findViewById(R.id.app_toolbar);
		coordinator = (CoordinatorLayout) findViewById(R.id.coordinator);

		setSupportActionBar(toolbar);

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

	private final ListObserver<Identity> idLoaded = new ListObserver<Identity>() {
		@Override
		public void added(Identity obj) {
			if (identity == null && obj.subscriber.equals(history.identity))
				go();
		}

		@Override
		public void removed(Identity obj) {

		}

		@Override
		public void updated(Identity obj) {

		}

		@Override
		public void reset() {
			go();
		}
	};

	private void popViewsTo(int offset, boolean configChange) {
		for (int j = viewStack.size() - 1; j >= offset; j--) {
			ViewState v = viewStack.get(j);
			// remove views from their containers (if required)
			IContainerView container = j > 0 ? viewStack.get(j - 1).getContainer() : this;
			container.deactivate(v, configChange);
			viewStack.remove(j);
		}
	}

	private void go() {
		HistoryItem item = history.getTop();
		Navigation n = item.key;
		Bundle args = item.args;

		if (identity == null && history.identity != null)
			identity = serval.identities.getIdentity(history.identity);

		if (n.requiresId && identity == null && !serval.identities.isLoaded()) {
			// go() again if the identity appears
			serval.identities.listObservers.add(idLoaded);
			n = Navigation.Spinner;
			args = null;
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
		}

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
		LayoutInflater inflater = LayoutInflater.from(this);
		ViewState parent = viewStack.isEmpty() ? null : viewStack.get(viewStack.size() - 1);
		while (n != null) {
			IContainerView container = (parent == null) ? this : parent.getContainer();
			if (container == null)
				throw new NullPointerException();
			parent = container.activate(n, identity, args);
			if (parent == null)
				throw new NullPointerException();
			viewStack.add(parent);
			if (newViews.empty())
				break;
			n = newViews.empty() ? null : newViews.pop();
		}

		ActionBar bar = getSupportActionBar();
		bar.invalidateOptionsMenu();
		bar.setDisplayOptions(
				ActionBar.DISPLAY_SHOW_HOME | (item.key.parent != null ? ActionBar.DISPLAY_HOME_AS_UP : 0),
				ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
	}

	public static Intent getIntentFor(Context context, Identity identity, Navigation key, Bundle args) {
		// Spawn new task
		Intent intent = new Intent();
		Class<?> activity = MainActivity.class;
		if (identity != null) {
			if (Build.VERSION.SDK_INT >= 21) {
				// Untested!
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
				intent.setDataAndType(Uri.parse("sid:" + identity.subscriber.sid.toHex()), "application/org.servalproject.sid");
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
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setClass(context, activity);
		intent.putExtras(NavHistory.prepareNew(identity == null ? null : identity.subscriber.sid, key, args));
		return intent;
	}

	public void go(Identity identity, Navigation key, Bundle args) {
		if (this.identity == identity) {
			go(key, args);
			return;
		}

		startActivity(getIntentFor(this, identity, key, args));
		if (Build.VERSION.SDK_INT >= 21)
			finishAndRemoveTask();
		else
			// TODO need an ugly work around here?
			finish();
	}

	public void go(Navigation key, Bundle args) {
		// record the change first, then create views
		if (history.add(key, args, false))
			go();
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

	@Override
	public void onBackPressed() {
		if (history.back()) {
			go();
		} else {
			super.onBackPressed();
		}
	}

	public boolean goUp() {
		if (history.up()) {
			go();
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (goUp())
					return true;
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private static final int SHARE = 1;

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
			case SHARE:
				try {
					File apk = new File(getApplicationInfo().sourceDir);
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(apk));
					intent.setType("image/apk");
					intent.addCategory(Intent.CATEGORY_DEFAULT);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				} catch (Exception e) {
					showError(e);
				}
				break;
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, SHARE, Menu.NONE, R.string.share_app)
				.setOnMenuItemClickListener(this);
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
	public void deactivate(ViewState state, boolean configChange) {
		ILifecycle lifecycle = state.getLifecycle();
		if (isStarted && lifecycle != null)
			lifecycle.onHidden();
		if (lifecycle != null)
			lifecycle.onDetach(configChange);
		rootLayout.removeView(state.view);
	}

	@Override
	public ViewState activate(Navigation n, Identity identity, Bundle args) {
		ViewState ret = ViewState.Inflate(this, n, identity, args);
		ret.view.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		rootLayout.addView(ret.view);
		ILifecycle lifecycle = ret.getLifecycle();
		if (isStarted && lifecycle != null)
			lifecycle.onVisible();
		// TODO observe identity and update title
		getSupportActionBar().setTitle(n.getTitle(this, identity));
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

	public void showError(final Exception e) {
		showSnack(e.getMessage(), Snackbar.LENGTH_LONG, getString(R.string.crash),
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						throw new IllegalStateException(e);
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
		Snackbar s = Snackbar.make(coordinator, message, length);
		if (action != null && actionLabel != null)
			s.setAction(actionLabel, action);
		s.show();
	}

}
