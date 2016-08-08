package org.servalproject.servalchat.navigation;

import android.content.Context;

import org.servalproject.mid.Identity;
import org.servalproject.servalchat.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jeremy on 14/06/16.
 */
public class Navigation {

    public final boolean requiresId;
    public final int titleResource;
    public final int layoutResource;
    public final Navigation parent;
    public final Navigation containedIn;
    public final String name;
    public final List<Navigation> children = new ArrayList<>();

    public Navigation(String name, boolean requiresId, int titleResource, int layoutResource, Navigation parent, Navigation containedIn){
        this.requiresId = requiresId;
        this.titleResource = titleResource;
        this.layoutResource = layoutResource;
        this.parent = parent;
        this.containedIn = containedIn;
        this.name = name;
        if (NavMap.containsKey(name))
            throw new IllegalStateException(name+" has already been defined");
        NavMap.put(name, this);
        if (containedIn != null)
            containedIn.children.add(this);
    }

    public Navigation(String name, int titleResource, int layoutResource){
        this(name, true, titleResource, layoutResource, null, null);
    }

    public Navigation(String name, int titleResource, int layoutResource, Navigation parent){
        this(name, true, titleResource, layoutResource, parent, null);
    }
    public Navigation(String name, int titleResource, int layoutResource, Navigation parent, Navigation containedIn){
        this(name, true, titleResource, layoutResource, parent, containedIn);
    }

    public CharSequence getTitle(Context context, Identity identity){
        if (titleResource == R.string.app_name && identity != null)
            return identity.getName();
        return context.getString(titleResource);
    }

    @Override
    public int hashCode() {
        int result = layoutResource;
        result = 31 * result + (parent != null ? parent.hashCode() : 0);
        return result;
    }

    public static final Map<String, Navigation> NavMap = new HashMap<>();
    // launcher..
    public static final Navigation IdentityList = new Navigation("IdentityList", false, R.string.my_details, R.layout.identity_list, null, null);
    public static final Navigation NewIdentityDetails = new Navigation("NewDetails", false, R.string.identity_details, R.layout.identity_details, IdentityList, null);

    // main screen
    // TODO sidebar menu for details, peer list, block list etc.
    public static final Navigation Spinner = new Navigation("Spinner", false, R.string.app_name, R.layout.progress, null, null);

    public static final Navigation Main = new Navigation("Main", R.string.app_name, R.layout.main_tabs);
    public static final Navigation IdentityDetails = new Navigation("Details", R.string.identity_details, R.layout.identity_details, null, Main);
    public static final Navigation MyFeed = new Navigation("MyFeed", R.string.my_feed, R.layout.my_feed, null, Main);
    public static final Navigation MyNews = new Navigation("MyNews", R.string.my_news, R.layout.placeholder, null, Main);
    public static final Navigation Inbox = new Navigation("Inbox", R.string.conversation_list, R.layout.conversation_list, null, Main);
    public static final Navigation Requests = new Navigation("Requests", R.string.requests, R.layout.placeholder, null, Main);
    public static final Navigation PeerList = new Navigation("PeerList", R.string.peer_list, R.layout.peer_list, null, Main);

    public static final Navigation PeerTabs = new Navigation("PeerTabs", R.string.app_name, R.layout.main_tabs);
    public static final Navigation PeerDetails = new Navigation("PeerDetails", R.string.peer_details, R.layout.peer_details, null, PeerTabs);
    public static final Navigation PeerFeed = new Navigation("PeerFeed", R.string.peer_feed, R.layout.peer_feed, null, PeerTabs);
    public static final Navigation PrivateMessages = new Navigation("PeerMessaging", R.string.message_list, R.layout.message_list, null, PeerTabs);
}
