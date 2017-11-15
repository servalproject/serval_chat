package org.servalproject.servalchat.peer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.servalproject.mid.Identity;
import org.servalproject.mid.MessageList;
import org.servalproject.mid.Peer;
import org.servalproject.servalchat.App;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.views.BackgroundWorker;
import org.servalproject.servalchat.views.BasicViewHolder;
import org.servalproject.servalchat.views.Presenter;
import org.servalproject.servalchat.views.PresenterFactory;
import org.servalproject.servalchat.views.ScrollingAdapter;
import org.servalproject.servalchat.views.TimestampView;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.meshms.MeshMSException;
import org.servalproject.servaldna.meshms.MeshMSMessage;

import java.io.IOException;

/**
 * Created by jeremy on 27/07/16.
 */
public final class PrivateMessagingPresenter extends Presenter<PrivateMessaging> {
	private MessageList messages;
	private final Peer peer;
	private static final String TAG = "PrivateMessaging";
	private boolean sending = false;
	private ScrollingAdapter<MeshMSMessage, ItemHolder> adapter;
	private MeshMSMessage delivered;

	private PrivateMessagingPresenter(PresenterFactory<PrivateMessaging, ?> factory, String key, Identity identity, Peer peer) {
		super(factory, key, identity);
		this.peer = peer;
	}

	public static PresenterFactory<PrivateMessaging, PrivateMessagingPresenter> factory
			= new PresenterFactory<PrivateMessaging, PrivateMessagingPresenter>() {

		@Override
		protected PrivateMessagingPresenter create(String key, Identity id, Peer peer) {
			return new PrivateMessagingPresenter(this, key, id, peer);
		}
	};

	@Override
	protected void bind(PrivateMessaging view) {
		view.send.setEnabled(!sending);
		view.list.setAdapter(adapter);
		if (App.isTesting() && "".equals(view.message.getText().toString()))
			view.message.setText("Sample Message \uD83D\uDE0E;");
	}

	@Override
	protected void restore(Bundle config) {
		// TODO show peer details?
		messages = identity.messaging.getPrivateMessages(peer.getSubscriber());
		adapter = new ScrollingAdapter<MeshMSMessage, ItemHolder>(messages) {
			@Override
			public ItemHolder create(ViewGroup parent, int viewType) {
				LayoutInflater inflater = LayoutInflater.from(parent.getContext());
				switch (MeshMSMessage.Type.values()[viewType]){
					default:
						return new TextItemHolder(inflater, parent, true);
					case MESSAGE_RECEIVED:
						return new TextItemHolder(inflater, parent, false);
					case ACK_RECEIVED:
						return new AckHolder(inflater, parent);
				}
			}

			@Override
			protected void addItem(int index, MeshMSMessage item) {
				if (item.type == MeshMSMessage.Type.ACK_RECEIVED) {
					if (delivered!=null)
						items.removeItem(delivered);
					delivered = item;
				}
				super.addItem(index, item);
			}

			@Override
			protected void bind(ItemHolder holder, MeshMSMessage item) {
			}

			@Override
			public void insertedItem(MeshMSMessage item, int position) {
				super.insertedItem(item, position);
				while((++position)+1<getItemCount()){
					if (getItem(position).type != MeshMSMessage.Type.ACK_RECEIVED){
						notifyItemChanged(position);
						break;
					}
				}
			}

			private MeshMSMessage previousMessage(int position){
				while(--position>=0){
					MeshMSMessage item = getItem(position);
					if (item.type!=MeshMSMessage.Type.ACK_RECEIVED)
						return item;
				}
				return null;
			}

			@Override
			protected void bindItem(ItemHolder holder, int position) {
				holder.bind(getItem(position), previousMessage(position));
			}

			@Override
			protected int getItemType(MeshMSMessage item) {
				return item.type.ordinal();
			}

			@Override
			protected MainActivity getActivity() {
				PrivateMessaging view = getView();
				return view == null ? null : view.activity;
			}

			@Override
			public long getItemId(int position) {
				MeshMSMessage item = getItem(position);
				if (item == null)
					return Long.MAX_VALUE;
				return item.getId();
			}
		};
		adapter.setHasStableIds(true);
	}

	private void markRead(){
		if (!messages.isRead()) {
			new BackgroundWorker() {
				@Override
				protected void onBackGround() throws Exception {
					messages.markRead();
				}

				@Override
				protected void onComplete(Throwable t) {
					if (t==null)
						return;
					PrivateMessaging view = getView();
					if (view!=null)
						view.activity.showError(t);
					else
						rethrow(t);
				}
			}.execute();
		}
	}

	@Override
	protected void onDestroy() {
		adapter.clear();
		markRead();
	}

	@Override
	public void onVisible() {
		super.onVisible();
		adapter.onVisible();
	}

	@Override
	public void onHidden() {
		super.onHidden();
		adapter.onHidden();
	}

	public void send() {
		PrivateMessaging view = getView();
		if (view == null)
			return;
		final String message = view.message.getText().toString();
		if ("".equals(message))
			return;

		sending = true;
		view.send.setEnabled(false);

		BackgroundWorker sender = new BackgroundWorker() {
			@Override
			protected void onBackGround() throws ServalDInterfaceException, MeshMSException, IOException {
				messages.sendMessage(message);
				messages.markRead();
			}

			@Override
			protected void onComplete(Throwable t) {
				sending = false;
				PrivateMessaging view = getView();
				if (view!=null) {
					view.send.setEnabled(true);
					if (t == null)
						view.message.setText("");
					else
						view.activity.showError(t);
				} else {
					rethrow(t);
				}
			}
		};
		sender.execute();
	}

	private abstract class ItemHolder extends BasicViewHolder {
		public ItemHolder(View itemView) {
			super(itemView);
		}
		public abstract void bind(MeshMSMessage item, MeshMSMessage previous);
	}

	private class TextItemHolder extends ItemHolder{
		TextView message;
		TimestampView age;

		public TextItemHolder(LayoutInflater inflater, ViewGroup parent, boolean myMessage) {
			super(inflater.inflate(myMessage ? R.layout.my_message : R.layout.their_message, parent, false));
			this.message = (TextView) this.itemView.findViewById(R.id.message);
			this.age = (TimestampView) this.itemView.findViewById(R.id.age);
		}

		public void bind(MeshMSMessage item, MeshMSMessage previous) {
			message.setText(item.text);
			age.setDates(item.date, previous==null?null:previous.date);
		}
	}

	private class AckHolder extends ItemHolder{
		public AckHolder(LayoutInflater inflater, ViewGroup parent) {
			super(inflater.inflate(R.layout.ack_message, parent, false));
		}

		@Override
		public void bind(MeshMSMessage item, MeshMSMessage previous) {
		}
	}
}
