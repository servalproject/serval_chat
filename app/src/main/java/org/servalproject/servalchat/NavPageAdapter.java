package org.servalproject.servalchat;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

/**
 * Created by jeremy on 8/06/16.
 */
public class NavPageAdapter extends PagerAdapter {

    private final Context context;
    private final Navigator navigator;
    final List<Navigation> screens;
    private int position=1;

    public NavPageAdapter(Context context, Navigator navigator, Navigation... screens) {
        this.context = context;
        this.navigator = navigator;
        this.screens = Arrays.asList(screens);
    }

    @Override
    public int getCount() {
        return screens.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Navigation screen = screens.get(position);
        View view = navigator.inflate(screen);
        container.addView(view);
        Navigator.navigated(view, screen);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = ((View) object);
        container.removeView(view);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return screens.get(position).getTitle(context);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        if (position!=this.position) {
            this.position = position;
            Navigation screen = screens.get(position);
            navigator.gotoView(screen);
        }
    }
}
