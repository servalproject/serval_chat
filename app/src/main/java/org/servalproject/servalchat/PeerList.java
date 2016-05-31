package org.servalproject.servalchat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
public class PeerList extends Fragment {
    private Serval serval;
    private RecyclerView list;
    private PeerListAdapter adapter;
    private static final String TAG = "PeerList";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        serval = Serval.getInstance();

        View root = inflater.inflate(R.layout.peer_list, container, false);

        list = (RecyclerView)root.findViewById(R.id.list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(root.getContext()));
        adapter = new PeerListAdapter();
        list.setAdapter(adapter);
        return root;
    }

    @Override
    public void onStop() {
        adapter.onPause();
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.onResume();
    }

    private class PeerHolder extends RecyclerView.ViewHolder{
        //private final ImageView avatar;
        private final TextView name;

        public PeerHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.peer, parent, false));
            //avatar = (ImageView)this.itemView.findViewById(R.id.avatar);
            name = (TextView)this.itemView.findViewById(R.id.name);
        }

        void bind(Peer p){
            name.setText(p.displayName());
            if (p.isReachable()){
                // Show green dot?
            }
        }
    }

    private class PeerListAdapter extends RecyclerView.Adapter<PeerHolder> implements ListObserver<Peer> {
        private boolean sorted = false;
        private final List<Peer> peers = new ArrayList<>();
        private final Set<SubscriberId> addedPeers = new HashSet<>();

        PeerListAdapter(){
            setHasStableIds(true);
        }

        @Override
        public PeerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new PeerHolder(parent);
        }

        private boolean add(Peer p){
            if (!p.isReachable())
                return false;
            if (addedPeers.contains(p.sid))
                return false;
            addedPeers.add(p.sid);
            peers.add(p);
            sorted = false;
            return true;
        }

        public void onResume(){
            peers.clear();
            addedPeers.clear();
            serval.knownPeers.peerListObservers.add(this);
            for (Peer p:serval.knownPeers.getKnownPeers())
                add(p);
            notifyDataSetChanged();
        }

        public void onPause() {
            serval.knownPeers.peerListObservers.remove(this);
            peers.clear();
        }

        @Override
        public void onBindViewHolder(PeerHolder holder, int position) {
            sort();
            holder.bind(peers.get(position));
        }

        private void sort(){
            if (sorted)
                return;
            Collections.sort(peers);
            sorted = true;
        }

        @Override
        public long getItemId(int position) {
            sort();
            return peers.get(position).getId();
        }

        @Override
        public int getItemCount() {
            return peers.size();
        }

        @Override
        public void added(Peer obj) {
            if (add(obj))
                notifyDataSetChanged();
        }

        @Override
        public void removed(Peer obj) {
        }

        @Override
        public void updated(Peer obj) {
            add(obj);
            sorted = false;
            notifyDataSetChanged();
        }
    }

}
