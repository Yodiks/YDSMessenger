package com.yds.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.yds.messenger.persistence.Persistence;

/**
 * Created by yds on 30/11/15.
 */
public class DebugActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        TextView registrationID = (TextView) findViewById(R.id.textView_RegistrationID);
        String regID = Persistence.getRegistrationID(this);
        if (regID != null) {
            registrationID.setText(regID);
        }
        String debugMessages = Persistence.getDebugMessages(this);
        debugMessages = "START:" + debugMessages + "\n:END";
        TextView debugMessagesField = (TextView) findViewById(R.id.textView_DebugMessages);
        debugMessagesField.setText(debugMessages);
        TextView storedKeys = (TextView) findViewById(R.id.textView_persistenceKeys);
        String[] keys = Persistence.getAllKeys(this);
        String keysStr = "";
        for (String k : keys) {
            keysStr += k + "\n";
        }
        storedKeys.setText(keysStr);
//        SharedPreferences shared = getSharedPreferences("RAVEN", MODE_PRIVATE);
//        storedKeys.setText(shared.getAll().toString());
        TextView secretUsername = (TextView) findViewById(R.id.textView_secretUsername);
        secretUsername.setText(Persistence.getSecretUsername(this));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.debug, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_clearDebugMessages) {
            Persistence.clearDebugMessages(this);
            Intent intent = new Intent(this, DebugActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
/**
 * Created by yds on 30/11/15.
 */