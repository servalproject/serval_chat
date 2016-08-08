package org.servalproject.mid;

import org.servalproject.servaldna.SigningKey;

/**
 * Created by jeremy on 3/08/16.
 */
public class MessageFeed implements IObservableList<MessageFeed.Message, Exception> {
    private final ListObserverSet<Message> observeFuture;
    private final SigningKey id;
    private int index=0;
    private int messageCount=100;

    public MessageFeed(Serval serval, SigningKey id) {
        this.observeFuture = new ListObserverSet<>(serval.uiHandler);
        this.id = id;
    }

    public void observe(ListObserver<Message> observer) {
        observeFuture.add(observer);
        // TODO start API call
    }

    public void stopObserving(ListObserver<Message> observer) {
        observeFuture.remove(observer);
        if (!observeFuture.hasObservers()) {
            // TODO stop API call
        }
    }

    public Message next(){
        if (index >= messageCount)
            return null;
        return new Message("Dummy message "+(index++));
    }

    @Override
    public void close() {
        // TODO
    }

    // stub class
    public class Message{
        public final String text;

        Message(String text){
            this.text = text;
        }
    }
}
