package org.servalproject.servalchat;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import org.servalproject.mid.Identity;

/**
 * Created by jeremy on 27/07/16.
 */
public class PrivateMessaging extends RelativeLayout
        implements INavigate, View.OnClickListener, IHaveMenu, MenuItem.OnMenuItemClickListener {
    MainActivity activity;
    EditText message;
    Button send;
    RecyclerView list;
    LinearLayoutManager layoutManager;
    PrivateMessagingPresenter presenter;

    public PrivateMessaging(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args) {
        this.activity = activity;
        this.message = (EditText)findViewById(R.id.message);
        this.send = (Button)findViewById(R.id.send);
        this.list = (RecyclerView)findViewById(R.id.message_list);
        layoutManager = new LinearLayoutManager(this.getContext());
        layoutManager.setReverseLayout(true);
        list.setLayoutManager(layoutManager);
        send.setOnClickListener(this);
        presenter = PrivateMessagingPresenter.factory.getPresenter(this, id, args);
        return presenter;
    }

    @Override
    public void onClick(View v) {
        if (v.getId()==R.id.send)
            presenter.send(message.getText().toString());
    }

    private static final int IGNORE = 1;
    private static final int BLOCK = 2;
    private static final int ADD = 3;

    @Override
    public void populateItems(Menu menu) {
        menu.add(Menu.NONE, ADD, Menu.NONE, R.string.add_contact)
                .setOnMenuItemClickListener(this);
        menu.add(Menu.NONE, IGNORE, Menu.NONE, R.string.ignore_contact)
                .setOnMenuItemClickListener(this);
        menu.add(Menu.NONE, BLOCK, Menu.NONE, R.string.block_contact)
                .setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case BLOCK:
                activity.showSnack("TODO, block contact", Snackbar.LENGTH_SHORT);
                break;
            case IGNORE:
                activity.showSnack("TODO, ignore contact", Snackbar.LENGTH_SHORT);
                break;
            case ADD:
                activity.showSnack("TODO, remember contact", Snackbar.LENGTH_SHORT);
                break;
            default:
                return false;
        }
        return true;
    }

}
