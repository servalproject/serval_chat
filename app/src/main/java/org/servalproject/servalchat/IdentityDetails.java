package org.servalproject.servalchat;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by jeremy on 7/06/16.
 */
public class IdentityDetails extends LinearLayout implements INavigate {
    TextView name;
    TextView phone;
    Button update;

    public IdentityDetails(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        name = (TextView)findViewById(R.id.name);
        phone = (TextView)findViewById(R.id.did);
        update = (Button)findViewById(R.id.update);
    }

    @Override
    public void onNavigate(Navigation n) {
        IdentityDetailsScreen screen = (IdentityDetailsScreen)n;
        screen.bind(this);
    }
}