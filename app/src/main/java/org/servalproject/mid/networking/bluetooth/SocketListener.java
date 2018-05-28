package org.servalproject.mid.networking.bluetooth;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

class SocketListener extends Thread {
	private BlueToothControl blueToothControl;
	private final BluetoothServerSocket socket;
	private final boolean secure;
	private boolean running = true;

	private static final String appName = "Serval";
	private static final String TAG = "BTListener";

	private SocketListener(BlueToothControl blueToothControl, BluetoothServerSocket socket, boolean secure, UUID uuid) {
		super(secure ? "BluetoothSL" : "BluetoothISL");
		this.blueToothControl = blueToothControl;
		this.secure = secure;
		this.socket = socket;
		this.start();
		Log.v(TAG, "Listening for; " + uuid);
	}

	public static SocketListener create(BlueToothControl blueToothControl, boolean secure, UUID uuid) throws IOException {
		if (!blueToothControl.adapter.isEnabled())
			return null;
		BluetoothServerSocket socket;
		if (secure){
			socket = blueToothControl.adapter.listenUsingRfcommWithServiceRecord(appName, uuid);
		}else{
			socket = blueToothControl.adapter.listenUsingInsecureRfcommWithServiceRecord(appName, uuid);
		}
		return new SocketListener(blueToothControl, socket, secure, uuid);
	}

	public void close() {
		try {
			running = false;
			socket.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	@Override
	public void run() {
		while (running && blueToothControl.adapter.isEnabled()) {
			try {
				BluetoothSocket client = socket.accept();
				Log.v(TAG, "Incoming connection from " + client.getRemoteDevice().getAddress());
				PeerState peer = blueToothControl.getPeer(client.getRemoteDevice());
				int bias = peer.device.getName().toLowerCase().compareTo(blueToothControl.adapter.getName().toLowerCase()) * -500;
				peer.onConnected(client, secure, bias);
			} catch (Exception e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}
}
