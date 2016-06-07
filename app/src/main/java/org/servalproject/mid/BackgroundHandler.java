package org.servalproject.mid;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by jeremy on 3/05/16.
 */
class BackgroundHandler extends Handler {
	private Serval serval;
	private static final String TAG="BackgroundHandler";

	private BackgroundHandler(Serval serval, Looper looper) {
		super(looper);
		this.serval = serval;
	}

	static BackgroundHandler create(Serval serval){
		HandlerThread handlerThread = new HandlerThread("BackgroundHandler");
		handlerThread.start();
		return new BackgroundHandler(serval, handlerThread.getLooper());
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
			case Serval.START:
				serval.startup();
				break;

			case Serval.SERVER_UP:
				serval.onServerStarted();
				break;

			case Serval.CPU_LOCK:
				serval.server.onCpuLock();
				break;
		}
	}

	public void replaceMessage(int what, int delay) {
		removeMessages(what);
		Message msg = obtainMessage(what);
		sendMessageDelayed(msg, delay);
	}
}
