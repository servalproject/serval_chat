package org.servalproject.servalchat.navigation;

import android.content.Context;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Peer;
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
	public final boolean requiresRhizome;
	public final int titleResource;
	public final int layoutResource;
	public final Navigation defaultParent;
	public final Navigation containedIn;
	public final String name;
	public final List<Navigation> children = new ArrayList<>();

	Navigation(String name, boolean requiresId, boolean requiresRhizome, int titleResource, int layoutResource, Navigation defaultParent, Navigation containedIn) {
		this.requiresId = requiresId;
		this.requiresRhizome = requiresRhizome;
		this.titleResource = titleResource;
		this.layoutResource = layoutResource;
		this.defaultParent = defaultParent;
		this.containedIn = containedIn;
		this.name = name;
		if (NavMap.containsKey(name))
			throw new IllegalStateException(name + " has already been defined");
		NavMap.put(name, this);
		if (containedIn != null)
			containedIn.children.add(this);
	}

	Navigation(String name, int titleResource, int layoutResource, Navigation containedIn) {
		this(name, true, true, titleResource, layoutResource, null, containedIn);
	}

	public CharSequence getTitle(Context context, Identity identity, Peer peer) {
		if (titleResource == R.string.app_name){
			if (peer != null)
				return peer.displayName();
			if (identity != null)
				return identity.getName();
		}
		return context.getString(titleResource);
	}

	public static final Map<String, Navigation> NavMap = new HashMap<>();
	public static final Navigation Root = new Navigation("Root", false, false, R.string.app_name, R.layout.main, null, null);

	// launcher..
	public static final Navigation Launcher = new Navigation("Launcher", false, false, R.string.app_name, R.layout.main_tabs, null, Root);
	public static final Navigation IdentityList = new Navigation("IdentityList", false, false, R.string.my_details, R.layout.identity_list, null, Launcher);
	public static final Navigation Networking = new Navigation("Networking", false, false, R.string.networking, R.layout.networking, null, Launcher);
	// TODO dialog...
	public static final Navigation NewIdentityDetails = new Navigation("NewDetails", false, false, R.string.identity_details, R.layout.identity_details, IdentityList, Root);

	// Block access to the requested navigation for some reason
	public static final Navigation Error = new Navigation("Error", false, false, R.string.app_name, R.layout.error, null, Root);

	// main screen
	public static final Navigation Main = new Navigation("Main", R.string.app_name, R.layout.main_sidebar, null);
	public static final Navigation IdentityDetails = new Navigation("Details", R.string.identity_details, R.layout.identity_details, Main);
	public static final Navigation MyFeed = new Navigation("MyFeed", R.string.my_feed, R.layout.my_feed, Main);
	public static final Navigation Inbox = new Navigation("Inbox", R.string.conversation_list, R.layout.conversation_list, Main);
	public static final Navigation AllFeeds = new Navigation("AllFeeds", R.string.all_feeds, R.layout.feed_list, Main);
	public static final Navigation Contacts = new Navigation("Contacts", R.string.contacts, R.layout.contacts, Main);
	public static final Navigation Requests = new Navigation("Requests", R.string.requests, R.layout.conversation_list, Main);
	public static final Navigation Blocked = new Navigation("Blocked", R.string.blocked, R.layout.block_list, Main);
	public static final Navigation PeerList = new Navigation("PeerList", R.string.peer_list, R.layout.peer_list, Main);
	public static final Navigation PeerMap = new Navigation("PeerMap", R.string.peer_map, R.layout.peer_map, Main);

	public static final Navigation PeerTabs = new Navigation("PeerTabs", R.string.app_name, R.layout.main_tabs, Root);
	public static final Navigation PeerDetails = new Navigation("PeerDetails", R.string.peer_details, R.layout.peer_details, PeerTabs);
	public static final Navigation PeerFeed = new Navigation("PeerFeed", R.string.peer_feed, R.layout.peer_feed, PeerTabs);
	public static final Navigation PrivateMessages = new Navigation("PeerMessaging", true, true, R.string.message_list, R.layout.message_list, Inbox, PeerTabs);
}
