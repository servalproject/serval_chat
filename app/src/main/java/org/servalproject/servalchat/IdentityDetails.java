package org.servalproject.servalchat;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.servalproject.mid.Identity;

/**
 * Created by jeremy on 7/06/16.
 */
public class IdentityDetails extends LinearLayout implements INavigate {
    Identity identity;
    TextView name;
    TextView phone;

    public IdentityDetails(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        name = (TextView)findViewById(R.id.name);
        phone = (TextView)findViewById(R.id.did);
    }

    @Override
    public void onNavigate(Navigation n) {
        IdentityDetailsScreen screen = (IdentityDetailsScreen)n;
        Identity id = screen.id;
        Context context = getContext();
        name.setText(id.getName(context));
        phone.setText(id.getDid(context));
    }
}