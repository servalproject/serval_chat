package org.servalproject.mid.networking.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;

/**
 * Created by jeremy on 7/04/15.
 */
class Connector implements Runnable{
	private final BlueToothControl control;
	private final BluetoothAdapter adapter;
	public final PeerState peer;
	private final boolean paired;
	private long connectionStarted=0;
	private boolean connecting = false;
	private BluetoothSocket socket = null;

	private static final String TAG = "Connector";

	Connector(BlueToothControl control, PeerState peer, boolean paired) {
		this.control = control;
		this.adapter = control.adapter;
		this.peer = peer;
		this.paired = paired;
	}

	@Override
	public void run() {
		connectionStarted = SystemClock.elapsedRealtime();

		try {
			if (paired)
				socket = peer.device.createRfcommSocketToServiceRecord(BlueToothControl.SECURE_UUID);
			else if (Build.VERSION.SDK_INT >= 10) {
				socket = peer.device.createInsecureRfcommSocketToServiceRecord(BlueToothControl.INSECURE_UUID);
			}
		} catch (IOException e){
			Log.v(TAG, "Failed to create socket", e);
		}

		if (socket!=null) {
			try {
				socket.connect();
				Log.v(TAG, "Connected to " + peer);
				int bias = peer.device.getName().toLowerCase().compareTo(control.adapter.getName().toLowerCase()) * 500;
				peer.onConnected(socket, paired, bias);
			} catch (IOException e) {
				try {
					socket.close();
				} catch (IOException e1) {
				}
				Log.v(TAG, "Connection failed to " + peer);
				peer.onConnectionFailed();
			}
			socket = null;
		}
		connecting = false;
		control.remove(this);
	}

	public void connect() {
		connecting = true;
		control.serval.runOnThreadPool(this);
	}

	public void cancel() {
		BluetoothSocket s = socket;
		if (s == null)
			return;
		try {
			Log.v(TAG, "Cancelling connection to " + peer);
			s.close();
		} catch (IOException e) {
		}
	}

	public synchronized void moveNext() {
		if (!connecting)
			connect();
		else if (connectionStarted!=0 && SystemClock.elapsedRealtime() - connectionStarted > 5000)
			cancel();
	}
}
