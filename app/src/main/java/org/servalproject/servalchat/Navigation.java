package org.servalproject.servalchat;

import android.content.Context;

import java.util.List;

/**
 * Created by jeremy on 14/06/16.
 */
public class Navigation {

    public final int titleResource;
    public final int layoutResource;
    public final Navigation parent;
    public final Navigation containedIn;
    public final String name;

    public Navigation(String name, int titleResource, int layoutResource, Navigation parent, Navigation containedIn){
        this.titleResource = titleResource;
        this.layoutResource = layoutResource;
        this.parent = parent;
        this.containedIn = containedIn;
        this.name = name;
    }

    public Navigation(String name, int layoutResource){
        this(name, -1, layoutResource, null, null);
    }

    public Navigation(String name, int layoutResource, Navigation parent){
        this(name, -1, layoutResource, parent, null);
    }

    public CharSequence getTitle(Context context){
        if (titleResource<=0)
            return name;
        return context.getString(titleResource);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Navigation that = (Navigation) o;

        if (layoutResource != that.layoutResource) return false;
        return parent != null ? parent.equals(that.parent) : that.parent == null;

    }

    @Override
    public int hashCode() {
        int result = layoutResource;
        result = 31 * result + (parent != null ? parent.hashCode() : 0);
        return result;
    }

    public static final Navigation Home = new Navigation("Home", R.layout.activity_main);
    public static final Navigation Identity = new Navigation("Identity", R.string.my_details, R.layout.identity_list, null, Home);
    public static final Navigation PeerList = new Navigation("PeerList", R.string.peer_list, R.layout.peer_list, null, Home);

    public static final Navigation HomeTabs[] = new Navigation[] {
            Identity,
            PeerList,
    };
}
