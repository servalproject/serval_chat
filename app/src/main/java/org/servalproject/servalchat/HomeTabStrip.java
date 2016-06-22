package org.servalproject.servalchat;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

/**
 * Created by jeremy on 7/06/16.
 */
public class HomeTabStrip extends LinearLayout implements IContainerView {
    final NavPageAdapter adapter;
    private ViewPager pager;

    public HomeTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        adapter =  new NavPageAdapter(context, Navigator.getNavigator(), Navigation.HomeTabs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        pager = (ViewPager)findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(adapter);
        TabLayout tabs = (TabLayout)findViewById(R.id.sliding_tabs);
        tabs.setupWithViewPager(pager);
    }

    @Override
    public void removeView(Navigation n) {
        // Noop, handled by viewpager
    }

    @Override
    public IContainerView addView(LayoutInflater inflater, Navigation n) {
        for(int i=0;i<adapter.screens.length;i++){
            Navigation screen = adapter.screens[i];
            if (screen.equals(n)){
                if (pager.getCurrentItem()!=i)
                    pager.setCurrentItem(i);
                return null;
            }
        }
        throw new IllegalStateException();
    }
}
