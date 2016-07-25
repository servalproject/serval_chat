package org.servalproject.servalchat;

import android.os.Bundle;

import org.servalproject.mid.Identity;

/**
 * Created by jeremy on 20/07/16.
 */
public abstract class Presenter<V> implements ILifecycle {
    private V view;

    public final Identity identity;
    protected Presenter(Identity identity){
        this.identity = identity;
    }

    protected final V getView(){
        return view;
    }

    public final void takeView(V view){
        this.view = view;
        if (view != null)
            bind();
    }

    protected void bind(){}

    void save(Bundle config){

    }

    void restore(Bundle config){

    }

    @Override
    public void onDetach() {
        takeView(null);
    }

    @Override
    public void onVisible() {

    }

    @Override
    public void onHidden() {

    }
}
