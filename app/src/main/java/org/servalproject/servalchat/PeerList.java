package org.servalproject.servalchat;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.servalproject.mid.ListObserver;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servaldna.SubscriberId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by jeremy on 31/05/16.
 */
public class PeerList extends RecyclerView
        implements ObservedListAdapter.Binder<Peer, PeerList.PeerHolder>{
    private Serval serval;
    private ObservedListAdapter<Peer, PeerHolder> adapter;
    private static final String TAG = "PeerList";
    private Navigator navigator;

    public PeerList(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        serval = Serval.getInstance();
        adapter = new PeerListAdapter(serval);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setHasFixedSize(true);
        setLayoutManager(new LinearLayoutManager(getContext()));
        setAdapter(adapter);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        navigator = Navigator.getNavigator();
        navigator.attachLifecycle(adapter);
    }

    @Override
    protected void onDetachedFromWindow() {
        navigator.detachLifecycle(adapter);
        navigator = null;
        super.onDetachedFromWindow();
    }

    @Override
    public PeerHolder createHolder(ViewGroup parent) {
        return new PeerHolder(parent);
    }

    @Override
    public void bind(PeerHolder holder, Peer item) {
        holder.name.setText(item.displayName());
        if (item.isReachable()){
            // Show green dot?
        }
    }

    @Override
    public long getId(Peer item){
        return item.getId();
    }

    public class PeerHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        //private final ImageView avatar;
        private final TextView name;

        public PeerHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.peer, parent, false));
            //avatar = (ImageView)this.itemView.findViewById(R.id.avatar);
            name = (TextView)this.itemView.findViewById(R.id.name);
            this.itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            // TODO
        }
    }

    private class PeerListAdapter extends ObservedListAdapter<Peer, PeerHolder>{
        private boolean sorted = false;
        private final Set<SubscriberId> addedPeers = new HashSet<>();

        PeerListAdapter(Serval serval){
            super(serval.knownPeers.peerListObservers, PeerList.this, new ArrayList<Peer>());
            setHasStableIds(true);
        }

        @Override
        public void onStart() {
            addedPeers.clear();
            items.clear();
            for(Peer p:serval.knownPeers.getReachablePeers())
                add(p);
            super.onStart();
        }

        @Override
        public void onStop() {
            super.onStop();
            addedPeers.clear();
            items.clear();
        }

        private boolean add(Peer p){
            if (!p.isReachable())
                return false;
            if (addedPeers.contains(p.sid))
                return false;
            addedPeers.add(p.sid);
            items.add(p);
            sorted = false;
            return true;
        }

        @Override
        protected Peer get(int position){
            sort();
            return super.get(position);
        }

        private void sort(){
            if (sorted)
                return;
            Collections.sort(items);
            sorted = true;
        }

        @Override
        public void added(Peer obj) {
            if (add(obj))
                notifyDataSetChanged();
        }

        @Override
        public void removed(Peer obj) {
            notifyDataSetChanged();
        }

        @Override
        public void updated(Peer obj) {
            add(obj);
            sorted = false;
            notifyDataSetChanged();
        }
    }

}
