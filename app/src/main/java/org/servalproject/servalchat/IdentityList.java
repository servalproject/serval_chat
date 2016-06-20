package org.servalproject.servalchat;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Serval;

import java.util.List;

/**
 * Created by jeremy on 1/06/16.
 */
public class IdentityList extends ObservedRecyclerView<Identity, IdentityList.IdentityHolder> {
    private Serval serval;
    private static final String TAG = "IdentityList";
    private Navigator navigator;
    private final List<Identity> identities;

    public IdentityList(Context context, @Nullable AttributeSet attrs) {
        super(Serval.getInstance().identities.listObservers, context, attrs);
        serval = Serval.getInstance();
        identities = serval.identities.getIdentities();
        navigator = Navigator.getNavigator();
        listAdapter.setHasStableIds(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setHasFixedSize(true);
        setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public IdentityHolder createHolder(ViewGroup parent) {
        return new IdentityHolder(parent);
    }

    @Override
    public void bind(IdentityHolder holder, Identity item) {
        holder.id = item;
        holder.name.setText(item.getName(holder.name.getContext()));
        boolean primary = (item==serval.identities.getSelected());
        holder.name.setTypeface(holder.name.getTypeface(), primary ? Typeface.BOLD : Typeface.NORMAL);
    }

    @Override
    protected Identity get(int position) {
        return identities.get(position);
    }

    @Override
    protected int getCount() {
        return identities.size();
    }

    @Override
    public long getId(Identity item){
        return item.getId();
    }

    public class IdentityHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView name;
        private Identity id;

        public IdentityHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.identity, parent, false));
            name = (TextView)this.itemView.findViewById(R.id.name);
            this.itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            navigator.gotoView(new IdentityDetailsScreen(id));
        }
    }
}
