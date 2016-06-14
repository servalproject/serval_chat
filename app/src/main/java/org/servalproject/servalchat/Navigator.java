package org.servalproject.servalchat;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Created by jeremy on 14/06/16.
 */
public class Navigator {
    private Navigator(){
    }

    private static final String TAG = "Navigator";
    private static Navigator instance;
    public static Navigator getNavigator(){
        return instance;
    }
    public static Navigator getNavigator(Activity activity){
        if (instance == null)
            instance = new Navigator();
        instance.setActivity(activity);
        return instance;
    }

    private boolean isStarted = false;
    private boolean isResumed = false;
    private Activity activity;
    private View rootView;
    public void setActivity(Activity activity) {
        this.activity = activity;
        viewStack.clear();
        current = null;
        rootView = null;
    }

    private Set<IActivityLifecycle> attached = new HashSet<>();
    public void attachLifecycle(IActivityLifecycle obj){
        attached.add(obj);
        if (isStarted)
            obj.onStart();
        if (isResumed)
            obj.onResume();
    }

    public void detachLifecycle(IActivityLifecycle obj){
        attached.remove(obj);
        if (isResumed)
            obj.onPause();
        if (isStarted)
            obj.onStop();
    }

    private final List<Navigation> backStack = new ArrayList<>();
    private final List<ViewState> viewStack = new ArrayList<>();
    private ViewState current;
    private boolean navigating = false;

    class ViewState{
        final IContainerView container;
        final Navigation navKey;

        ViewState(IContainerView container, Navigation navKey){
            this.container = container;
            this.navKey = navKey;
        }
    }

    // eg tabstrip navigation
    private void recordChangedView(Navigation obj){
        for (int i=0;i<backStack.size(); i++) {
            Navigation n = backStack.get(i);
            if (n.equals(obj)){
                // pop everything else
                Log.v(TAG, "Popping backstack to "+obj.getTitle(activity));
                while(backStack.size()>i+1)
                    backStack.remove(i+1);
                return;
            }
        }
        Log.v(TAG, "Pushing onto backstack; "+obj.getTitle(activity));
        backStack.add(obj);
    }

    public static IContainerView findContainer(View v){
        if (v instanceof IContainerView)
            return (IContainerView)v;
        if (v instanceof ViewGroup){
            ViewGroup g = (ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                View child = g.getChildAt(i);
                IContainerView container = findContainer(child);
                if (container!=null)
                    return container;
            }
        }
        return null;
    }

    public static void navigated(View view, Navigation n){
        if (view instanceof ViewGroup){
            ViewGroup g = (ViewGroup)view;
            for(int i=0;i<g.getChildCount();i++) {
                navigated(g.getChildAt(i), n);
            }
        }
        if (view instanceof INavigate)
            ((INavigate)view).onNavigate(n);
    }

    public View inflate(Navigation n){
        LayoutInflater inflater = LayoutInflater.from(activity);
        return inflater.inflate(n.layoutResource, null);
    }

    public void gotoView(Navigation obj){
        Log.v(TAG, "Navigating to "+obj.getTitle(activity));
        if (current != null && current.navKey.equals(obj)) {
            Log.v(TAG, "Noop");
            return;
        }

        if (navigating)
            return;
        navigating = true;

        Stack<Navigation> newViews = new Stack<>();
        Navigation n = obj;
        while(n != null){
            Log.v(TAG, "Pushing nav "+n.getTitle(activity));
            newViews.push(n);
            n = n.containedIn;
        }

        // ignore common parent views
        n = newViews.pop();
        if (n==null)
            throw new IllegalStateException();
        Log.v(TAG, "Testing for common containers; "+n.getTitle(activity));
        int i=0;
        while (i<viewStack.size() && viewStack.get(i).navKey.equals(n)){
            Log.v(TAG, "Ignoring common container "+i+" "+n.getTitle(activity));
            i++;
            n = newViews.pop();
        }

        // pop un-common views
        for (int j=viewStack.size()-1; j>=i; j--){
            ViewState v = viewStack.get(j);
            // remove views from their containers (if required)
            Log.v(TAG, "Popping view "+j+", "+v.navKey.getTitle(activity));
            if (j>0){
                viewStack.get(j -1).container.removeView(v.navKey);
            }else{
                rootView = null;
            }
            viewStack.remove(j);
        }

        // add views (& locate containers?)
        LayoutInflater inflater = LayoutInflater.from(activity);
        ViewState parent = viewStack.isEmpty() ? null : viewStack.get(viewStack.size()-1);
        while(true){
            Log.v(TAG, "Inflating  "+n.getTitle(activity));
            IContainerView container;
            if (parent == null){
                View v = inflater.inflate(n.layoutResource, null);
                rootView = v;
                activity.setContentView(v);
                container = findContainer(v);
                navigated(v, n);
            }else{
                if (parent.container==null)
                    throw new IllegalStateException();
                container = parent.container.addView(inflater, n);
            }
            parent = new ViewState(container, n);
            viewStack.add(parent);
            if (newViews.empty())
                break;
            n = newViews.pop();
        }
        if (rootView==null)
            throw new IllegalStateException();
        current = parent;
        recordChangedView(parent.navKey);
        navigating = false;
    }

    public void gotoIntent(Intent i){
        // TODO
    }

    public void gotoTop(){
        gotoView(backStack.get(backStack.size()-1));
    }

    public boolean goBack() {
        backStack.remove(backStack.size()-1);
        if (backStack.isEmpty())
            return false;
        gotoTop();
        return true;
    }

    public void onStop() {
        isStarted = false;
        for(IActivityLifecycle l:attached)
            l.onStop();
    }

    public void onStart() {
        if (backStack.isEmpty())
            gotoView(Navigation.Identity);
        else
            gotoTop();
        isStarted = true;
        for(IActivityLifecycle l:attached)
            l.onStart();
    }

    public void onPause() {
        isResumed = false;
        for(IActivityLifecycle l:attached)
            l.onPause();
    }

    public void onResume() {
        isResumed = true;
        for(IActivityLifecycle l:attached)
            l.onResume();
    }
}
