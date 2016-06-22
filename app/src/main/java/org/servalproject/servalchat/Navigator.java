package org.servalproject.servalchat;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.Menu;
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
    public static Navigator getNavigator(Context context, INavigatorHost container){
        if (instance == null)
            instance = new Navigator();
        instance.onDestroy();
        instance.context = context;
        instance.container = container;
        return instance;
    }

    private boolean isStarted = false;
    private boolean isResumed = false;
    private Context context;
    private INavigatorHost container;

    public void onDestroy(){
        this.context = null;
        viewStack.clear();
        current = null;
        container = null;
    }

    private Set<IActivityLifecycle> lifecycle = new HashSet<>();
    private Set<IHaveMenu> menus = new HashSet<>();

    public void onAttach(View obj){
        if (obj instanceof IActivityLifecycle){
            IActivityLifecycle l = (IActivityLifecycle)obj;
            lifecycle.add(l);
            if (isStarted)
                l.onStart();
            if (isResumed)
                l.onResume();
        }
        if (obj instanceof ViewGroup){
            ViewGroup g = (ViewGroup)obj;
            for(int i=0;i<g.getChildCount();i++)
                onAttach(g.getChildAt(i));
        }
    }

    public void onDetach(View obj){
        if (obj instanceof IActivityLifecycle){
            IActivityLifecycle l = (IActivityLifecycle)obj;
            lifecycle.remove(l);
            if (isResumed)
                l.onPause();
            if (isStarted)
                l.onStop();
        }
        if (obj instanceof ViewGroup){
            ViewGroup g = (ViewGroup)obj;
            for(int i=0;i<g.getChildCount();i++)
                onDetach(g.getChildAt(i));
        }
    }

    public void onActivate(View obj, Navigation n){
        if (obj instanceof IHaveMenu) {
            menus.add((IHaveMenu) obj);
            container.rebuildMenu();
        }
        if (obj instanceof INavigate)
            ((INavigate)obj).onNavigate(n);
        if (obj instanceof ViewGroup){
            ViewGroup g = (ViewGroup)obj;
            for(int i=0;i<g.getChildCount();i++)
                onActivate(g.getChildAt(i), n);
        }
    }

    public void onDeactivate(View obj){
        if (obj instanceof IHaveMenu) {
            menus.remove((IHaveMenu) obj);
            container.rebuildMenu();
        }
        if (obj instanceof ViewGroup){
            ViewGroup g = (ViewGroup)obj;
            for(int i=0;i<g.getChildCount();i++)
                onDeactivate(g.getChildAt(i));
        }
    }

    private final List<Navigation> backStack = new ArrayList<>();
    private final List<ViewState> viewStack = new ArrayList<>();
    private ViewState current;
    private boolean navigating = false;

    public boolean populateMenu(Menu menu) {
        if (menus.isEmpty())
            return false;
        for (IHaveMenu m:menus)
            m.populateItems(menu);
        return true;
    }

    public void showError(final Exception e) {
        container.showSnack(e.getMessage(), Snackbar.LENGTH_LONG, context.getString(R.string.crash),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    public void showPopup(CharSequence label, CharSequence actionLabel, View.OnClickListener action){
        container.showSnack(label, Snackbar.LENGTH_LONG, actionLabel, action);
    }

    public void showPopup(CharSequence label){
        container.showSnack(label, Snackbar.LENGTH_SHORT, null, null);
    }

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
                while(backStack.size()>i+1)
                    backStack.remove(i+1);
                return;
            }
        }
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

    public View inflate(Navigation n){
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(n.layoutResource, null);
    }

    public boolean isCurrentView(Navigation obj){
        return current != null && current.navKey.equals(obj);
    }

    public void gotoView(Navigation obj){
        if (current != null && current.navKey.equals(obj))
            return;

        if (navigating)
            return;
        navigating = true;

        Stack<Navigation> newViews = new Stack<>();
        Navigation n = obj;
        while(n != null){
            newViews.push(n);
            n = n.containedIn;
        }

        // ignore common parent views
        n = newViews.pop();
        if (n==null)
            throw new IllegalStateException();
        int i=0;

        while (i<viewStack.size() && viewStack.get(i).navKey.equals(n)){
            i++;
            n = newViews.pop();
        }

        // pop un-common views
        for (int j=viewStack.size()-1; j>=i; j--){
            ViewState v = viewStack.get(j);
            // remove views from their containers (if required)
            IContainerView container = j>0 ? viewStack.get(j -1).container : this.container;
            container.removeView(v.navKey);
            viewStack.remove(j);
        }

        // add views (& locate containers?)
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewState parent = viewStack.isEmpty() ? null : viewStack.get(viewStack.size()-1);
        while(true){
            IContainerView container = (parent == null) ? this.container : parent.container;
            if (container == null)
                throw new IllegalStateException();
            container = container.addView(inflater, n);
            parent = new ViewState(container, n);
            viewStack.add(parent);
            if (newViews.empty())
                break;
            n = newViews.pop();
        }
        current = parent;
        recordChangedView(current.navKey);
        this.container.navigated(backStack.size()>1, current.navKey.parent != null);
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

    public boolean goUp() {
        if (current==null || current.navKey.parent == null)
            return false;
        gotoView(current.navKey.parent);
        return true;
    }

    public void onStop() {
        isStarted = false;
        for(IActivityLifecycle v: lifecycle)
            v.onStop();
    }

    public void onStart() {
        if (backStack.isEmpty())
            gotoView(Navigation.Identity);
        else
            gotoTop();
        isStarted = true;
        for(IActivityLifecycle v: lifecycle)
            v.onStart();
    }

    public void onPause() {
        isResumed = false;
        for(IActivityLifecycle v: lifecycle)
            v.onPause();
    }

    public void onResume() {
        isResumed = true;
        for(IActivityLifecycle v: lifecycle)
            v.onResume();
    }
}
