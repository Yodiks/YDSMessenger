package com.yds.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.yds.messenger.models.Contact;
import com.yds.messenger.persistence.Persistence;
/**
 * Created by yds on 30/11/15.
 */

public class SelfDestructingMessageDisplay extends ActionBarActivity {

    TextView contactNameField, remainingTime, messageField;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_destructing_message_display);

        contactNameField = (TextView) findViewById(R.id.textView_contactName);
        remainingTime = (TextView) findViewById(R.id.textView_remainingTime);
        messageField = (TextView) findViewById(R.id.textView_message);

        Intent thisIntent = getIntent();
        String secretUsername = thisIntent.getStringExtra("secretUsername");
        final int destroyAfter = thisIntent.getIntExtra("destroyAfter", 20);
        String message = thisIntent.getStringExtra("message");

        Contact contact = Persistence.getUser(this, secretUsername);
        contactNameField.setText(contact.username);
        remainingTime.setText("" + destroyAfter + " " + getString(R.string.seconds));
        messageField.setText(message);

        handler = new Handler();
        Thread countDown = new Thread(new Runnable() {
            int timeLimit = destroyAfter;

            @Override
            public void run() {
                while (timeLimit > 0) {
                    try {
                        Thread.sleep(1000);
                        timeLimit--;
                        setRemainingTime(timeLimit);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                dismiss();
            }
        });
        countDown.start();
    }

    public void setRemainingTime(final int seconds) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                remainingTime.setText("" + seconds + " " + getString(R.string.seconds));
            }
        });
    }

    public void dismiss() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                SelfDestructingMessageDisplay.this.finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.self_destructing_message_display, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == 0) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
/**
 * Created by yds on 30/11/15.
 */