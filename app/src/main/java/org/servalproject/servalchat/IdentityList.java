package org.servalproject.servalchat;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Serval;

/**
 * Created by jeremy on 1/06/16.
 */
public class IdentityList extends RecyclerView
        implements ObservedListAdapter.Binder<Identity,IdentityList.IdentityHolder> {
    private Serval serval;
    private Navigator navigator;
    private ObservedListAdapter<Identity, IdentityHolder> adapter;
    private static final String TAG = "IdentityList";

    public IdentityList(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        serval = Serval.getInstance();
        navigator = Navigator.getNavigator();
        adapter = new ObservedListAdapter<>(serval.identities.listObservers, this, serval.identities.getIdentities());
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
        navigator.attachLifecycle(adapter);
    }

    @Override
    protected void onDetachedFromWindow() {
        navigator.detachLifecycle(adapter);
        super.onDetachedFromWindow();
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
    public long getId(Identity item){
        return 0;
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
