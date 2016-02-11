package com.yds.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ListView;

import com.yds.messenger.listadapters.MessageListAdapter;
import com.yds.messenger.models.Contact;
import com.yds.messenger.models.MessageListItem;
import com.yds.messenger.persistence.Persistence;
/**
 * Created by yds on 30/11/15.
 */

public class ShareViaYds extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_via_raven);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        String prepopulatedMessage = "Incompatible content";
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                prepopulatedMessage = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
        }

        Contact[] contacts = Persistence.getContacts(this);
        MessageListItem[] messages = new MessageListItem[contacts.length];
        for (int i = 0; i < messages.length; i++) {
            messages[i] = new MessageListItem();
            messages[i].contactName = contacts[i].username;
            messages[i].messagePreview = "This is a long message sent by contact " + (i + 1);
            messages[i].secretUsername = contacts[i].secretUsername;
            messages[i].registrationID = contacts[i].registrationID;
        }

        ListView messagesList = (ListView) findViewById(R.id.listView_MessageList);
        MessageListAdapter mla = new MessageListAdapter(this, messages, prepopulatedMessage);
        messagesList.setAdapter(mla);

    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

}
/**
 * Created by yds on 30/11/15.
 */