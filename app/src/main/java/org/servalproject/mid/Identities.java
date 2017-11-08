package org.servalproject.mid;

import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.SigningKey;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.SubscriberId;
import org.servalproject.servaldna.keyring.KeyringIdentity;
import org.servalproject.servaldna.keyring.KeyringIdentityList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jeremy on 6/06/16.
 */
public class Identities {
	private static final String TAG = "Identities";
	private final Serval serval;
	private boolean loaded = false;
	public final ListObserverSet<Identity> listObservers;
	private final List<Identity> identityList = new ArrayList<>();
	private final Map<Subscriber, Identity> identities = new HashMap<>();
	private Identity selected;

	Identities(Serval serval) {
		this.serval = serval;
		listObservers = new ListObserverSet<>(serval);
	}

	void onStart() {
		enterPin(null);
	}

	private Identity addId(KeyringIdentity id) {
		Identity i = identities.get(id.subscriber);
		if (i == null) {
			i = new Identity(serval, id.subscriber);
			identities.put(id.subscriber, i);
			identityList.add(i);
			if (selected == null)
				selected = i;
			i.update(id);
			listObservers.onAdd(i);
		} else {
			i.update(id);
			listObservers.onUpdate(i);
		}
		return i;
	}

	public List<Identity> getIdentities() {
		return identityList;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public Identity getIdentity(SigningKey key) {
		for (Identity id : identityList) {
			if (id.subscriber.signingKey.equals(key))
				return id;
		}
		return null;
	}

	public void updateIdentity(final Identity id, final String did, final String name, final String pin) throws ServalDInterfaceException, IOException {
		addId(serval.getResultClient().keyringSetDidName(id.subscriber, did, name, pin));
	}

	public Identity addIdentity(final String did, final String name, final String pin) throws ServalDInterfaceException, IOException {
		return addId(serval.getResultClient().keyringAdd(did, name, pin));
	}

	void enterPin(final String pin) {
		serval.runOnThreadPool(new Runnable() {
			@Override
			public void run() {
				try {
					KeyringIdentityList list = serval.getResultClient().keyringListIdentities(pin);
					KeyringIdentity id = null;
					while ((id = list.nextIdentity()) != null)
						addId(id);
					if (pin == null)
						loaded = true;
					listObservers.onReset();
				} catch (ServalDInterfaceException |
						IOException e) {
					throw new IllegalStateException(e);
				}
			}
		});
	}
}
