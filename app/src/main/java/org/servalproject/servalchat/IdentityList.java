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
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.ListObserver;
import org.servalproject.mid.Serval;

/**
 * Created by jeremy on 1/06/16.
 */
public class IdentityList extends Fragment {
    private Serval serval;
    private RecyclerView list;
    private IdentityListAdapter adapter;
    private static final String TAG = "IdentityList";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        serval = Serval.getInstance();

        View root = inflater.inflate(R.layout.identity_list, container, false);

        list = (RecyclerView)root.findViewById(R.id.list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(root.getContext()));
        adapter = new IdentityListAdapter();
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

    private class IdentityHolder extends RecyclerView.ViewHolder{
        //private final ImageView avatar;
        private final TextView name;
        private final TextView did;

        public IdentityHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.identity, parent, false));
            name = (TextView)this.itemView.findViewById(R.id.name);
            did = (TextView)this.itemView.findViewById(R.id.did);
        }

        void bind(Identity i){
            name.setText(i.getName(name.getContext()));
            did.setText(i.getDid(did.getContext()));
        }
    }

    private class IdentityListAdapter extends RecyclerView.Adapter<IdentityHolder> implements ListObserver<Identity> {
        public void onResume() {
            serval.identities.listObservers.add(this);
            notifyDataSetChanged();
        }

        public void onPause(){
            serval.identities.listObservers.remove(this);
        }

        @Override
        public IdentityHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new IdentityHolder(parent);
        }

        @Override
        public void onBindViewHolder(IdentityHolder holder, int position) {
            holder.bind(serval.identities.getIdentities().get(position));
        }

        @Override
        public int getItemCount() {
            return serval.identities.getIdentities().size();
        }

        @Override
        public void added(Identity obj) {
            notifyDataSetChanged();
        }

        @Override
        public void removed(Identity obj) {
            notifyDataSetChanged();
        }

        @Override
        public void updated(Identity obj) {
            notifyDataSetChanged();
        }
    }

}
