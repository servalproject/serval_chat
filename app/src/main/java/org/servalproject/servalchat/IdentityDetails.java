package org.servalproject.servalchat;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.servalproject.mid.Identity;

/**
 * Created by jeremy on 7/06/16.
 */
public class IdentityDetails extends LinearLayout
        implements INavigate, View.OnClickListener {
    MainActivity activity;
    TextView name;
    TextView phone;
    Button update;
    IdentityDetailsPresenter presenter;

    public IdentityDetails(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args) {
        this.activity = activity;
        name = (TextView)findViewById(R.id.name);
        phone = (TextView)findViewById(R.id.did);
        update = (Button)findViewById(R.id.update);
        update.setOnClickListener(this);

        presenter = IdentityDetailsPresenter.factory.getPresenter(this, id, args);
        return presenter;
    }

    @Override
    public void onClick(View v) {
        presenter.update();
    }

}