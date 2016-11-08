package org.servalproject.mid.networking.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import org.servalproject.mid.Serval;
import org.servalproject.mid.networking.NetworkInfo;
import org.servalproject.servalchat.R;

/**
 * Created by jeremy on 8/11/16.
 */

public class BlueToothInfo extends NetworkInfo {
	private final BlueToothControl control;

	BlueToothInfo(BlueToothControl control, Serval serval) {
		super(serval);
		this.control = control;
	}
	@Override
	public String getName(Context context) {
		return context.getString(R.string.bluetooth);
	}

	@Override
	public String getStatus(Context context) {
		if (getState()==State.On && !control.isDiscoverable())
			return context.getString(R.string.not_discoverable);
		return super.getStatus(context);
	}

	@Override
	public void enable(Context context) {
		control.requestDiscoverable(context);
	}

	@Override
	public void disable(Context context) {
		if (control.isEnabled())
			control.adapter.disable();
	}

	@Override
	public void toggle(Context context) {
		if (!control.isDiscoverable())
			enable(context);
		else
			disable(context);
	}

	static State statusToState(int status){
		switch (status){
			case BluetoothAdapter.STATE_ON:
				return State.On;
			case BluetoothAdapter.STATE_OFF:
				return State.Off;
			case BluetoothAdapter.STATE_TURNING_ON:
				return State.Starting;
			case BluetoothAdapter.STATE_TURNING_OFF:
				return State.Stopping;
			default:
				return State.Error;
		}
	}

	void setState(int state) {
		setState(statusToState(state));
	}
}
