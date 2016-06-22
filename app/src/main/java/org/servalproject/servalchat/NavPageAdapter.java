package org.servalproject.servalchat;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by jeremy on 8/06/16.
 */
public class NavPageAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {

    private final Context context;
    private final Navigator navigator;
    final Navigation[] screens;
    private int position = -1;
    private final View[] views;

    public NavPageAdapter(Context context, Navigator navigator, Navigation... screens) {
        this.context = context;
        this.navigator = navigator;
        this.screens = screens;
        this.views = new View[screens.length];
    }

    @Override
    public int getCount() {
        return screens.length;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Navigation screen = screens[position];
        View view = views[position] = navigator.inflate(screen);
        container.addView(view);
        navigator.onAttach(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = ((View) object);
        navigator.onDetach(view);
        container.removeView(view);
        views[position] = null;
    }

    private void pageChanged(int position){
        if (position == this.position)
            return;

        if (this.position != -1)
            navigator.onDeactivate(views[this.position]);

        this.position = position;
        navigator.onActivate(views[position], screens[position]);
        navigator.gotoView(screens[position]);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        pageChanged(position);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return screens[position].getTitle(context);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        pageChanged(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
