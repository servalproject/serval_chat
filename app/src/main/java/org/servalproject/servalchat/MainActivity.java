package org.servalproject.servalchat;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.servalproject.mid.KnownPeers;
import org.servalproject.mid.ListObserver;
import org.servalproject.mid.ListObserverSet;
import org.servalproject.mid.Observer;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.mid.Server;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Serval serval;
    private static final String TAG = "MainActivity";
    private RecyclerView list;
    private PeerListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serval = Serval.getInstance();
        setContentView(R.layout.activity_main);
        list = (RecyclerView)findViewById(R.id.list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PeerListAdapter();
        list.setAdapter(adapter);
    }

    private class PeerHolder extends RecyclerView.ViewHolder implements Observer<Peer> {
        private final ImageView avatar;
        private final TextView name;

        public PeerHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.peer, parent, false));
            avatar = (ImageView)this.itemView.findViewById(R.id.avatar);
            name = (TextView)this.itemView.findViewById(R.id.name);
        }

        private Peer p;
        void bind(Peer p){
            this.p = p;
            updated(p);
            p.observers.add(this);
        }

        void recycled(){
            p.observers.remove(this);
            p=null;
        }

        @Override
        public void updated(Peer obj) {
            String n = obj.getName();
            if (n==null || "".equals(n))
                n = obj.getDid();
            if (n==null || "".equals(n))
                n = obj.sid.abbreviation();
            name.setText(n);
            if (obj.isReachable()){
                // Show green dot?
            }
        }
    }

    private class PeerListAdapter extends RecyclerView.Adapter<PeerHolder> implements ListObserver<Peer>{
        private final List<Peer> peers = new ArrayList<>();

        PeerListAdapter(){
            setHasStableIds(true);
        }

        @Override
        public PeerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new PeerHolder(parent);
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            serval.knownPeers.peerListObservers.add(this);
            peers.clear();
            for (Peer p:serval.knownPeers.getKnownPeers())
                if (p.isReachable())
                    peers.add(p);
            // TODO sort peers?
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            serval.knownPeers.peerListObservers.remove(this);
            peers.clear();
        }

        @Override
        public void onBindViewHolder(PeerHolder holder, int position) {
            holder.bind(peers.get(position));
        }

        @Override
        public void onViewRecycled(PeerHolder holder) {
            super.onViewRecycled(holder);
            holder.recycled();
        }

        @Override
        public long getItemId(int position) {
            return peers.get(position).getId();
        }

        @Override
        public int getItemCount() {
            return peers.size();
        }

        @Override
        public void added(Peer obj) {
            peers.add(obj);
            notifyItemInserted(peers.size() - 1);
        }

        @Override
        public void removed(Peer obj) {
            // TODO?
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        serval.server.observers.remove(serverState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        serval.server.observers.add(serverState);
   }

    private Observer<Server> serverState = new Observer<Server>() {
        @Override
        public void updated(Server server) {
            // TODO?
        }
    };
}
