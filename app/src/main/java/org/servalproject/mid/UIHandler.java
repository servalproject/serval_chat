package org.servalproject.mid;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by jeremy on 11/05/16.
 */
public class UIHandler extends Handler {

    UIHandler(){
        super(Looper.getMainLooper());
    }

    @Override
    public void handleMessage(Message msg) {
        Object c = msg.obj;
        if (c!=null && c instanceof UIMessage<?>){
            UIMessage<?> m = (UIMessage<?>)c;
            m.handleMessage();
            return;
        }
        super.dispatchMessage(msg);
    }

    public <T> void sendMessage(MessageHandler<T> h, T obj){
        // TODO reuse UIMessage instances?
        UIMessage<T> m = new UIMessage<>(h, obj);
        Message msg = obtainMessage(0, m);
        sendMessage(msg);
    }

    private class UIMessage<T>{
        final MessageHandler<T> handler;
        final T obj;
        UIMessage(MessageHandler<T> handler, T obj){
            this.handler = handler;
            this.obj = obj;
        }
        void handleMessage(){
            handler.handleMessage(obj);
        }
    }

    public interface MessageHandler<T>{
        void handleMessage(T obj);
    }
}
