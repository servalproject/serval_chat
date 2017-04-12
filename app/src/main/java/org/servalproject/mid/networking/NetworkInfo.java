package org.servalproject.mid.networking;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.servalproject.mid.ObserverSet;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;

/**
 * Created by jeremy on 2/11/16.
 */
public abstract class NetworkInfo {
	private static final String TAG = "NetworkInfo";
	public enum State{
		Off(R.string.stopped),
		Starting(R.string.starting),
		Stopping(R.string.stopping),
		On(R.string.started),
		Error(R.string.unknown_error);

		public final int stringResource;
		State(int stringResource){
			this.stringResource = stringResource;
		}

		public String getString(Context context){
			if (stringResource == 0)
				throw new IllegalArgumentException();
			return context.getString(stringResource);
		}
	}

	public final ObserverSet<NetworkInfo> observers;
	protected final Serval serval;

	public NetworkInfo(Serval serval){
		this.serval = serval;
		observers = new ObserverSet<>(serval, this);
	}

	public abstract String getName(Context context);
	public abstract void enable(Context context);
	public abstract void disable(Context context);
	public abstract Intent getIntent(Context context);

	public boolean isUsable(){
		return isOn();
	}

	public boolean isOn(){
		switch (getState()){
			case On:
				return true;
			default:
				return false;
		}
	}

	public void toggle(Context context){
		if (isUsable())
			disable(context);
		else
			enable(context);
	}

	public String getStatus(Context context){
		return getState().getString(context);
	}

	private State state = State.Error;
	protected void setState(State state){
		if (state == this.state)
			return;
		Log.v(TAG, getName(serval.context)+" changed to "+state);
		this.state = state;
		observers.onUpdate();
	}

	public State getState(){
		return state;
	}
}
