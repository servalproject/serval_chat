package org.servalproject.mid;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by jeremy on 11/05/16.
 */
public class CallbackHandler extends Handler {

	CallbackHandler(Looper looper) {
		super(looper);
	}

	@Override
	public void handleMessage(Message msg) {
		Object c = msg.obj;
		if (c != null){
			if (c instanceof CallbackMessage<?>) {
				CallbackMessage<?> m = (CallbackMessage<?>) c;
				m.handleMessage(msg);
				return;
			}else if(c instanceof Callback){
				((Callback)c).handleMessage(msg);
				return;
			}
		}
		super.dispatchMessage(msg);
	}

	public void sendEmptyMessage(Callback callback, int what){
		Message msg = obtainMessage(what, callback);
		sendMessage(msg);
	}

	public <T> void sendMessage(MessageHandler<T> h, T obj, int what) {
		if (isOnThread()) {
			// call immediately if already in the right thread
			h.handleMessage(obj, what);
		} else {
			// TODO reuse CallbackMessage instances?
			CallbackMessage<T> m = new CallbackMessage<>(h, obj);
			Message msg = obtainMessage(what, m);
			sendMessage(msg);
		}
	}

	public boolean isOnThread() {
		return Thread.currentThread() == this.getLooper().getThread();
	}

	private class CallbackMessage<T> {
		final MessageHandler<T> handler;
		final T obj;

		CallbackMessage(MessageHandler<T> handler, T obj) {
			this.handler = handler;
			this.obj = obj;
		}

		void handleMessage(Message msg) {
			handler.handleMessage(obj, msg.what);
		}
	}

	public interface MessageHandler<T> {
		void handleMessage(T obj, int what);
	}
}
