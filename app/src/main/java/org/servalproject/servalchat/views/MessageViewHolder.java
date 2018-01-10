package org.servalproject.servalchat.views;

import android.view.ViewGroup;

import org.servalproject.servalchat.R;

/**
 * Created by jeremy on 10/01/18.
 */

public class MessageViewHolder extends BasicViewHolder {
	public MessageViewHolder(int textResource, ViewGroup parent) {
		super(R.layout.error, parent);
		DisplayError e = (DisplayError)itemView.findViewById(R.id.error);
		e.setText(textResource);
	}
}
