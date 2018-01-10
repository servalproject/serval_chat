package org.servalproject.servalchat.views;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by jeremy on 30/01/17.
 */

public abstract class BasicViewHolder extends RecyclerView.ViewHolder {
	public BasicViewHolder(View itemView) {
		super(itemView);
	}
	public BasicViewHolder(LayoutInflater inflater, int layout_id, ViewGroup parent){
		this(inflater.inflate(layout_id, parent, false));
	}
	public BasicViewHolder(int layout_id, ViewGroup parent){
		this(LayoutInflater.from(parent.getContext()), layout_id, parent);
	}
}
