package org.servalproject.servalchat;

import org.servalproject.mid.Identity;

/**
 * Created by jeremy on 8/06/16.
 */
public class IdentityDetailsScreen extends Navigation {
    public final Identity id;

    public IdentityDetailsScreen(Identity id) {
        super("IdentityDetails", R.string.identity_details, R.layout.identity_details, Navigation.Identity);
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IdentityDetailsScreen that = (IdentityDetailsScreen) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }
}
