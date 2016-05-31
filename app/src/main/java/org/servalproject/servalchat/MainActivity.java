package org.servalproject.servalchat;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PageAdapter adapter = new PageAdapter(getSupportFragmentManager());
        ViewPager pager = (ViewPager)findViewById(R.id.pager);
        pager.setAdapter(adapter);

        TabLayout tabs = (TabLayout)findViewById(R.id.sliding_tabs);
        tabs.setupWithViewPager(pager);

    }

    private class PageAdapter extends FragmentPagerAdapter{

        public PageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 3:
                    return new PeerList();
            }
            Placeholder p = new Placeholder();
            p.label = getPageTitle(position);
            return p;
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position){
                case 0:
                    return getString(R.string.my_details);
                case 1:
                    return getString(R.string.feed);
                case 2:
                    return getString(R.string.requests);
                case 3:
                    return getString(R.string.peer_list);
                case 4:
                    return getString(R.string.blocked);
            }
            return super.getPageTitle(position);
        }
    }
}
