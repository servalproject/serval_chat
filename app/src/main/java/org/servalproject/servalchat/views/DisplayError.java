package org.servalproject.servalchat.views;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Peer;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;

/**
 * Created by jeremy on 8/01/18.
 */

public class DisplayError extends AppCompatTextView implements INavigate {

	public DisplayError(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public static final String MESSAGE = "message";

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args) {
		if (args!=null){
			int res = args.getInt(MESSAGE, -1);
			if (res!=-1)
				setText(res);
		}
		return null;
	}
}
