package org.servalproject.mid.networking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.Characteristics;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.servalproject.mid.Serval;
import org.servalproject.servaldna.AbstractExternalInterface;
import org.servalproject.servaldna.ChannelSelector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.O)
public class WifiAware {
	private final Serval serval;
	private final WifiAwareManager manager;
	private WifiAwareSession session;
	private PublishDiscoverySession publishSession;
	private SubscribeDiscoverySession subscribeSession;
	private static final String TAG = "WifiAware";
	private static final String SERVICE = "org.servalproject";
	private int msgId=0;
	private Map<PeerHandle, Integer> peerMap = new HashMap<>();
	private List<PeerHandle> peerList = new ArrayList<>();
	private int MTU;

	public static WifiAware getWifiAware(Serval serval, ChannelSelector selector, int loopbackMdpPort) {
		if (!serval.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
			Log.v(TAG, "Wifi aware feature not found");
			return null;
		}

		try {
			return new WifiAware(serval, selector, loopbackMdpPort);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private final AbstractExternalInterface externalInterface;

	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED.equals(action)){
				onStateChanged();
			}

		}
	};

	private final AttachCallback attach = new AttachCallback(){
		@Override
		public void onAttached(WifiAwareSession s) {
			super.onAttached(s);
			setSession(s);
		}

		@Override
		public void onAttachFailed() {
			super.onAttachFailed();
			setSession(null);
			Log.v(TAG, "Attach failed!");
		}
	};

	private final DiscoverySessionCallback publish = new DiscoverySessionCallback(){
		@Override
		public void onPublishStarted(@NonNull PublishDiscoverySession session) {
			super.onPublishStarted(session);
			setPublishSession(session);
		}

		@Override
		public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
			super.onSubscribeStarted(session);
			setSubscribeSession(session);
		}

		@Override
		public void onSessionConfigUpdated() {
			super.onSessionConfigUpdated();
			Log.v(TAG, "Session config updated");
		}

		@Override
		public void onSessionConfigFailed() {
			super.onSessionConfigFailed();
			Log.v(TAG, "Session config failed");
		}

		@Override
		public void onSessionTerminated() {
			super.onSessionTerminated();
			Log.v(TAG, "Session terminated");
		}

		@Override
		public void onServiceDiscovered(final PeerHandle peerHandle, final byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
			super.onServiceDiscovered(peerHandle, serviceSpecificInfo, matchFilter);
			Log.v(TAG, "Peer service discovered!");
			serval.runOnThreadPool(new Runnable() {
				@Override
				public void run() {
					try {
						byte[] peer = peerAddr(peerHandle);
						if (serviceSpecificInfo == null || serviceSpecificInfo.length ==0) {
							externalInterface.discovered(peer);
						}else {
							externalInterface.receivedPacket(peer, serviceSpecificInfo);
						}
					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			});
		}

		@Override
		public void onMessageSendSucceeded(int messageId) {
			super.onMessageSendSucceeded(messageId);
			Log.v(TAG, "Message "+messageId+" sent");
		}

		@Override
		public void onMessageSendFailed(int messageId) {
			super.onMessageSendFailed(messageId);
			Log.v(TAG, "Message "+messageId+" failed");
		}

		@Override
		public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
			super.onMessageReceived(peerHandle, message);
			Log.v(TAG, "Message received");
		}
	};

	public WifiAware(Serval serval, ChannelSelector selector, int loopbackMdpPort) throws IOException {
		this.serval = serval;
		manager = serval.context.getSystemService(WifiAwareManager.class);
		externalInterface = new AbstractExternalInterface(selector, loopbackMdpPort) {
			@Override
			protected void sendPacket(byte[] addr, ByteBuffer payload) {
				onSendPacket(addr, payload);
			}
		};
		Characteristics c = manager.getCharacteristics();
		Log.v(TAG, "Max service name len: "+c.getMaxServiceNameLength());
		Log.v(TAG, "Max service info len: "+(MTU = c.getMaxServiceSpecificInfoLength()));
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);
		serval.context.registerReceiver(receiver, filter);
		onStateChanged();
	}

	private byte[] peerAddr(PeerHandle handle){
		Integer index = peerMap.get(handle);
		if (index == null){
			index = peerList.size();
			peerList.add(handle);
			peerMap.put(handle, index);
		}
		return new byte[]{
			(byte) (index&0xff),
			(byte) ((index>>8)&0xff),
			(byte) ((index>>16)&0xff),
			(byte) ((index>>24)&0xff)
		};
	}

	private PeerHandle peerHandle(byte[] addr){
		int index = addr[0] |
				((addr[1]&0xff)<<8) |
				((addr[2]&0xff)<<16) |
				((addr[3]&0xff)<<24);
		return peerList.get(index);
	}

	private void onSendPacket(byte[] addr, ByteBuffer payload) {
		PublishDiscoverySession s = publishSession;
		if (s!=null){
			byte[] packet = new byte[payload.remaining()];
			payload.get(packet);
			if (addr == null || addr.length == 0) {
				PublishConfig publishConfig = new PublishConfig.Builder()
						.setPublishType(PublishConfig.PUBLISH_TYPE_SOLICITED)
						.setServiceName(SERVICE)
						.setServiceSpecificInfo(packet)
						.build();
				Log.v(TAG, "update publish config");
				s.updatePublish(publishConfig);
			}else{
				Log.v(TAG, "Sending message "+(++msgId));
				s.sendMessage(peerHandle(addr), msgId, packet);
			}
		}
	}

	private Runnable down = new Runnable() {
		@Override
		public void run() {
			try {
				externalInterface.down();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	};

	private Runnable up = new Runnable() {
		@Override
		public void run() {
			try {
				externalInterface.up(
						"socket_type=EXTERNAL\n" +
								"match=wifiaware\n" +
								"prefer_unicast=off\n" +
								"idle_tick_ms=120000\n"+
								"broadcast.mtu="+MTU+"\n" +
								"broadcast.tick_ms=10000\n" +
								"broadcast.packet_interval=500000\n" +
								"broadcast.reachable_timeout_ms=30000\n" +
								"unicast.mtu="+MTU+"\n" +
								"unicast.tick_ms=10000\n" +
								"unicast.packet_interval=500000\n" +
								"unicast.reachable_timeout_ms=10000\n"
				);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	};

	private void setPublishSession(PublishDiscoverySession s){
		if (publishSession == s)
			return;
		if (publishSession != null) {
			publishSession.close();
			Log.v(TAG, "Publish session closed");
		}
		boolean changed = (publishSession == null) != (s == null);
		publishSession = s;
		if (s!=null)
			Log.v(TAG, "Publish session created");
		if (changed){
			serval.runOnThreadPool((s == null) ? down : up);
		}
	}

	private void setSubscribeSession(SubscribeDiscoverySession s){
		if (subscribeSession== s)
			return;
		if (subscribeSession!= null) {
			subscribeSession.close();
			Log.v(TAG, "Subscribe session closed");
		}
		subscribeSession = s;
		if (s!=null){
			Log.v(TAG, "Subscribe session created");
		}
	}

	private void setSession(WifiAwareSession s){
		if (s == this.session)
			return;

		if (this.session != null){
			session.close();
			Log.v(TAG, "session closed");
		}

		session = s;

		if (s!=null) {
			Log.v(TAG, "Attached to session");

			PublishConfig publishConfig = new PublishConfig.Builder()
					.setPublishType(PublishConfig.PUBLISH_TYPE_SOLICITED)
					.setServiceName(SERVICE)
					// TODO .setServiceSpecificInfo() ?
					.build();
			session.publish(publishConfig, publish, serval.backgroundHandler);
			SubscribeConfig subscribeConfig = new SubscribeConfig.Builder()
					.setServiceName(SERVICE)
					.setSubscribeType(SubscribeConfig.SUBSCRIBE_TYPE_PASSIVE)
					// TODO .setServiceSpecificInfo() ?
					.build();
			session.subscribe(subscribeConfig, publish, serval.backgroundHandler);
		}
	}

	private void onStateChanged() {
		boolean available = manager.isAvailable();

		if (available && session == null)
			manager.attach(attach, serval.backgroundHandler);
		if (!available) {
			setPublishSession(null);
			setSubscribeSession(null);
			setSession(null);
		}
	}

}
