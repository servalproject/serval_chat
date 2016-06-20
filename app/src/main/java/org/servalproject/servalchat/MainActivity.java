package org.servalproject.servalchat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity implements INavigatorHost {

    private Navigator navigator;
    private View activeView;
    private LinearLayout rootLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        rootLayout = (LinearLayout) findViewById(R.id.root_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_toolbar);
        setSupportActionBar(toolbar);

        navigator = Navigator.getNavigator(this, this);
        Intent i = getIntent();
        if (i!=null)
            navigator.gotoIntent(i);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        navigator.gotoIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO add navigator backstack?
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigator.onDestroy();
        navigator = null;
    }

    @Override
    public void onBackPressed() {
        if (!navigator.goBack())
            super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                if (navigator.goUp())
                    return true;
                break;
            // TODO menu items per view?
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // TODO tweak menu properties here
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu) || navigator.populateMenu(menu);
    }

    @Override
    protected void onStop() {
        super.onStop();
        navigator.onStop();
    }

    @Override
    protected void onStart() {
        navigator.onStart();
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        navigator.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigator.onResume();
    }

    @Override
    public void removeView(Navigation n) {
        if (activeView==null)
            throw new IllegalStateException();
        navigator.onDeactivate(activeView);
        navigator.onDetach(activeView);
        rootLayout.removeView(activeView);
        activeView = null;
    }

    @Override
    public IContainerView addView(LayoutInflater inflater, Navigation n) {
        if (activeView!=null)
            throw new IllegalStateException();
        activeView = inflater.inflate(n.layoutResource, null);
        rootLayout.addView(activeView);
        IContainerView ret = Navigator.findContainer(activeView);
        navigator.onAttach(activeView);
        navigator.onActivate(activeView, n);
        getSupportActionBar().setTitle(n.getTitle(this));
        return ret;
    }

    @Override
    public void navigated(boolean backEnabled, boolean upEnabled) {
        getSupportActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_HOME | (upEnabled ? ActionBar.DISPLAY_HOME_AS_UP : 0),
                ActionBar.DISPLAY_SHOW_HOME|ActionBar.DISPLAY_HOME_AS_UP);
    }

    @Override
    public void rebuildMenu() {
        getSupportActionBar().invalidateOptionsMenu();
    }
}
