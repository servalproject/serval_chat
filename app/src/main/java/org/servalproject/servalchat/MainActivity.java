package org.servalproject.servalchat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Navigator navigator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navigator = Navigator.getNavigator(this);
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
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigator.setActivity(null);
        navigator = null;
    }

    @Override
    public void onBackPressed() {
        if (!navigator.goBack())
            super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        // TODO
        return super.onSupportNavigateUp();
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
}
