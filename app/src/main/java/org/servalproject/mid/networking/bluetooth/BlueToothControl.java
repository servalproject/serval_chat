package org.servalproject.mid.networking.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import org.servalproject.mid.Serval;
import org.servalproject.mid.networking.NetworkInfo;
import org.servalproject.servalchat.App;
import org.servalproject.servaldna.AbstractExternalInterface;
import org.servalproject.servaldna.ChannelSelector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by jeremy on 9/02/15.
 */
public class BlueToothControl extends AbstractExternalInterface {
	final BluetoothAdapter adapter;
	final Serval serval;
	private final HashMap<String, PeerState> peers = new HashMap<String, PeerState>();
	private SocketListener secureListener, insecureListener;
	static final int MTU = 1200;
	private static final String TAG = "BlueToothControl";
	private static final String SERVAL_PREFIX = "Serval:";
	private static final String BLUETOOTH_NAME = "bluetoothName";
	public final BlueToothInfo networkInfo;
	public final Scanner scanner;

	// chosen by fair dice roll (otherwise known as UUID.randomUUID())
	static final UUID SECURE_UUID = UUID.fromString("85d832c2-b7e9-4166-a65f-695b925485aa");
	static final UUID INSECURE_UUID = UUID.fromString("4db52983-2c1b-454e-a8ba-e8fb4ae59eeb");

	public static BlueToothControl getBlueToothControl(
			Serval serval,
			ChannelSelector selector,
			int loopbackMdpPort) {
		BluetoothAdapter a;
		if (Build.VERSION.SDK_INT>=18) {
			BluetoothManager bm = (BluetoothManager)serval.context.getSystemService(Context.BLUETOOTH_SERVICE);
			a = bm.getAdapter();
		}else
			a = BluetoothAdapter.getDefaultAdapter();
		if (a == null) return null;

		try {
			return new BlueToothControl(
					serval,
					selector,
					loopbackMdpPort,
					a);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private BlueToothControl(final Serval serval,
							 ChannelSelector selector,
							 int loopbackMdpPort,
							 BluetoothAdapter a) throws IOException {
		super(selector, loopbackMdpPort);
		this.serval = serval;
		adapter = a;
		this.scanner = new Scanner(this, a);
		String oldName = serval.settings.getString(BLUETOOTH_NAME, "");
		if (!"".equals(oldName)){
			String name = adapter.getName();
			if (name.startsWith(SERVAL_PREFIX))
				adapter.setName(oldName);
			SharedPreferences.Editor ed = serval.settings.edit();
			ed.putString(BLUETOOTH_NAME, "");
			ed.apply();
		}
		networkInfo = new BlueToothInfo(this, serval);
		networkInfo.setState(adapter.getState());
	}

	public static final int TICK_MS = 15000;
	public static final int TIMEOUT = TICK_MS*2;

	private final Runnable up = new Runnable() {
		@Override
		public void run() {
			try {
				StringBuilder sb = new StringBuilder();

				sb.append("socket_type=EXTERNAL\n")
						.append("match=bluetooth\n")
						.append("prefer_unicast=on\n")
						.append("broadcast.send=false\n")
						.append("unicast.tick_ms="+TICK_MS+"\n")
						.append("unicast.reachable_timeout_ms="+TIMEOUT+"\n")
						.append("idle_tick_ms=120000\n");
				up(sb.toString());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	};

	private final Runnable listen = new Runnable() {
		@Override
		public void run() {
			if (secureListener != null)
				return;
			try {
				secureListener = SocketListener.create(BlueToothControl.this, true, SECURE_UUID);
				insecureListener = SocketListener.create(BlueToothControl.this, false, INSECURE_UUID);
			} catch (Exception e) {
				Log.e(TAG, e.getMessage(), e);
			}
			up.run();
			// Immediately try to connect to paired devices
			for(BluetoothDevice d : adapter.getBondedDevices())
				peerFound(d);

			serval.runReplaceDelayed(scan, 1000);
		}
	};

	private final Runnable stopListening = new Runnable() {
		@Override
		public void run() {
			if (secureListener == null)
				return;

			synchronized (peers) {
				for (PeerState p : peers.values()) {
					p.disconnect();
				}
				peers.clear();
			}
			queue.clear();

			if (secureListener != null) {
				secureListener.close();
				secureListener = null;
			}
			if (insecureListener != null) {
				insecureListener.close();
				insecureListener = null;
			}

			try {
				down();
			} catch (IOException e) {
				Log.e(TAG, e.getMessage(), e);
			}

			Log.v(TAG, "Stopped listening");
		}
	};

	public PeerState getPeer(BluetoothDevice device) {
		PeerState s = this.peers.get(device.getAddress());
		if (s == null) {
			s = new PeerState(this, device, getAddress(device));
			synchronized (peers) {
				this.peers.put(device.getAddress(), s);
			}
		}
		return s;
	}

	private byte[] getAddress(BluetoothDevice device) {
		// TODO convert mac address string to hex bytes?
		return device.getAddress().getBytes();
	}

	private PeerState getDevice(byte[] address) {
		// TODO convert mac address string to hex bytes?
		String addr = new String(address);
		PeerState ret = peers.get(addr);
		if (ret == null)
			Log.v(TAG, "Unable to find bluetooth device for " + addr);
		return ret;
	}

	private static Charset UTF8 = Charset.forName("UTF-8");

	@Override
	protected void sendPacket(byte[] addr, ByteBuffer payload) {
		// TODO do we need a wakelock?
		if (addr == null || addr.length == 0)
			return;
		PeerState peer = getDevice(addr);
		if (peer == null)
			return;
		byte payloadBytes[] = new byte[payload.remaining()];
		payload.get(payloadBytes);
		peer.queuePacket(payloadBytes);
	}

	public void onFound(Intent intent) {
		BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		peerFound(device);
		scanner.onPeerScanned();
	}

	void discovered(PeerState peer) throws IOException {
		discovered(peer.addrBytes);
	}

	private void peerFound(BluetoothDevice device){
		final PeerState peer = getPeer(device);

		if (peer.shouldConnect())
			peer.connect();
	}

	public void onRemoteNameChanged(Intent intent) {
		scanner.onPeerScanned();
	}

	private void setState(int state) {
		networkInfo.setState(state);
		scanner.setState();
		if (networkInfo.getState() == NetworkInfo.State.On) {
			serval.runOnThreadPool(listen);
		} else {
			serval.runOnThreadPool(stopListening);
		}
	}

	public void onEnableChanged() {
		setState(adapter.getState());
	}

	public void onStateChange(Intent intent) {
		// on / off etc
		setState(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0));
		if (adapter.isEnabled())
			serval.runOnThreadPool(up);
	}

	// pull the interface up / down
	public void setEnabled(boolean enabled) {
		NetworkInfo.State state = networkInfo.getState();
		if (state == NetworkInfo.State.On && !enabled)
			adapter.disable();
		if (state == NetworkInfo.State.Off && enabled)
			adapter.enable();
	}

	public boolean isDiscoverable() {
		return isEnabled() && adapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
	}

	public boolean isEnabled() {
		return adapter.isEnabled();
	}

	public void requestDiscoverable(Context context) {
		if (isDiscoverable())
			return;

		if (App.isTesting()){
			// snack?
			return;
		}
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
		context.startActivity(discoverableIntent);
	}

	private Queue<Connector> queue = new LinkedList<>();
	public synchronized void queue(Connector item){
		boolean runNow = !queue.isEmpty() && !scanner.isDiscovering();
		queue.add(item);
		if (runNow)
			runNext();
	}

	public synchronized void remove(Connector item){
		queue.remove(item);
		runNext();
	}

	private Runnable scan = new Runnable() {
		@Override
		public void run() {
			runNext();
		}
	};

	public synchronized void runNext() {
		if (!adapter.isEnabled())
			return;

		int delay = scanner.nextScanAction(queue.peek());
		serval.runReplaceDelayed(scan, delay);
	}
}
