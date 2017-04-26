package org.servalproject.servalchat.network;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Observer;
import org.servalproject.mid.Serval;
import org.servalproject.mid.networking.NetworkInfo;
import org.servalproject.mid.networking.Networks;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servalchat.views.ObservedRecyclerView;
import org.servalproject.servalchat.views.RecyclerHelper;
import org.servalproject.servalchat.views.SimpleRecyclerView;

/**
 * Created by jeremy on 2/11/16.
 */
public class NetworkList extends ObservedRecyclerView<NetworkInfo, NetworkList.NetworkHolder> {
	private final Serval serval;
	private static final String TAG = "NetworkList";

	public NetworkList(Context context, @Nullable AttributeSet attrs) {
		super(null, context, attrs);
		serval = Serval.getInstance();
		setHasFixedSize(true);
		RecyclerHelper.createLayoutManager(this, true, false);
		RecyclerHelper.createDivider(this);
		setObserverSet(Networks.getInstance().observers);
	}

	@Override
	protected NetworkHolder createHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.network, parent, false);
		return new NetworkHolder(view);
	}

	@Override
	protected void bind(NetworkHolder holder, NetworkInfo item) {
		holder.setItem(item);
	}

	@Override
	protected NetworkInfo get(int position) {
		return Networks.getInstance().networks.get(position);
	}

	@Override
	protected int getCount() {
		return Networks.getInstance().networks.size();
	}

	@Override
	protected void unBind(NetworkHolder holder, NetworkInfo item) {
		super.unBind(holder, item);
		holder.setItem(null);
	}

	public class NetworkHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		TextView name;
		TextView status;
		ImageView icon;
		SwitchCompat onOff;

		NetworkInfo info;

		public NetworkHolder(View itemView) {
			super(itemView);
			name = (TextView)itemView.findViewById(R.id.name);
			status = (TextView)itemView.findViewById(R.id.status);
			icon = (ImageView)itemView.findViewById(R.id.icon);
			onOff = (SwitchCompat)itemView.findViewById(R.id.onoff);
			itemView.setOnClickListener(this);
			onOff.setOnClickListener(this);
		}

		public void setItem(NetworkInfo info){
			this.info = info;
			if (info != null){
				Context context = getContext();
				String name = info.getName(context);
				String status = info.getStatus(context);
				Log.v(TAG, "setItem "+name+" - "+status);
				this.name.setText(name);
				this.status.setText(status);
				this.onOff.setChecked(info.isUsable());

				Intent i = info.getIntent(context);
				Drawable d = null;
				if (i!=null) {
					PackageManager packageManager = getContext().getPackageManager();
					ResolveInfo r = packageManager.resolveActivity(i, 0);
					if (r != null)
						d = r.loadIcon(packageManager);
				}
				icon.setVisibility((d==null) ? View.GONE : View.VISIBLE);
				icon.setImageDrawable(d);
			}
		}

		@Override
		public void onClick(View view) {
			if (info != null) {
				switch (view.getId()){
					case R.id.onoff:
						info.toggle(getContext());
						break;
					default:
						Intent i = info.getIntent(getContext());
						if (i!=null)
							getContext().startActivity(i);
				}
			}
		}
	}
}
