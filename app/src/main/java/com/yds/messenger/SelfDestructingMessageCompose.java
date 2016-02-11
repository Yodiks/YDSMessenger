package com.yds.messenger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.yds.messenger.models.Contact;
import com.yds.messenger.models.MessageTypes;
import com.yds.messenger.persistence.Persistence;
import com.yds.messenger.utils.MessageDispatcher;
import com.yds.messenger.utils.crypto.EncryptionUtils;

import org.json.JSONException;
import org.json.JSONObject;
/**
 * Created by yds on 30/11/15.
 */

public class SelfDestructingMessageCompose extends ActionBarActivity {

    String targetSecretUsername;
    String targetUsername;
    String mySecretUsername;
    String targetRegId;

    TextView contactNameField;
    EditText messageField;
    RadioGroup durationRadioGroup;
    int customValue = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sefl_destructing_message_compose);

        Intent thisIntent = getIntent();
        targetSecretUsername = thisIntent.getStringExtra("secretUsername");
        targetUsername = thisIntent.getStringExtra("username");
        mySecretUsername = Persistence.getSecretUsername(this);
        targetRegId = thisIntent.getStringExtra("registrationID");

        contactNameField = (TextView) findViewById(R.id.textView_contactName);
        durationRadioGroup = (RadioGroup) findViewById(R.id.radioGroup_destroy_after);
        messageField = (EditText) findViewById(R.id.editText_newMessageText);

        contactNameField.setText(targetUsername);

        RadioButton customButton = (RadioButton) findViewById(R.id.radioButton_custom);
        customButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, boolean b) {
                if (b) {
                    final EditText input = new EditText(SelfDestructingMessageCompose.this);
                    input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    new AlertDialog.Builder(SelfDestructingMessageCompose.this)
                            .setTitle(R.string.custom_time)
                            .setMessage(R.string.pick_custom_time)
                            .setView(input)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    int value;
                                    try {
                                        value = Integer.parseInt(input.getText().toString().trim());
                                        customValue = value;
                                        compoundButton.setText(value + " (" + getString(R.string.destroy_after_custom) + ")");
                                    } catch (NumberFormatException nfe) {
                                        Toast.makeText(SelfDestructingMessageCompose.this, R.string.not_a_number, Toast.LENGTH_SHORT).show();
                                        RadioButton btn5sec = (RadioButton) findViewById(R.id.radioButton_5sec);
                                        btn5sec.setChecked(true);
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Toast.makeText(SelfDestructingMessageCompose.this, R.string.cancelled, Toast.LENGTH_SHORT).show();
                                    RadioButton btn5sec = (RadioButton) findViewById(R.id.radioButton_5sec);
                                    btn5sec.setChecked(true);
                                }
                            }).show();
                }
            }
        });

        ImageButton sendButton = (ImageButton) findViewById(R.id.button_newMessageSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String receiverSecretUsername = targetSecretUsername;
                final String receiverRegID = targetRegId;
                final String mySecretUsernameStr = mySecretUsername;
                String tempMessage = messageField.getText().toString();

                //Check if message has to be encrypted. If yes, encrypt it.
                Contact user = Persistence.getUser(SelfDestructingMessageCompose.this, targetSecretUsername);
                if (user.encKey != null) {
                    tempMessage = EncryptionUtils.encrypt(tempMessage, user.encKey);
                }

                final String message = tempMessage;

                if (message.trim().equals("")) {
                    Toast.makeText(SelfDestructingMessageCompose.this, R.string.empty_message, Toast.LENGTH_SHORT).show();
                    return;
                }
                int duration;
                switch (durationRadioGroup.getCheckedRadioButtonId()) {
                    case R.id.radioButton_20sec:
                        duration = 20;
                        break;
                    case R.id.radioButton_5sec:
                        duration = 5;
                        break;
                    case R.id.radioButton_10sec:
                        duration = 10;
                        break;
                    case R.id.radioButton_custom:
                        duration = customValue;
                        break;
                    default:
                        duration = 10;
                }
                final int destroyAfter = duration;

                new AsyncTask() {
                    ProgressDialog pd;

                    @Override
                    protected Object doInBackground(Object[] objects) {
                        JSONObject messageJSON = new JSONObject();
                        String messageStr = "";
                        try {
                            messageJSON.put("secretUsername", mySecretUsername);
                            messageJSON.put("message", message);
                            messageJSON.put("destroyAfter", destroyAfter);
                            messageStr = messageJSON.toString();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return MessageDispatcher.dispatchMessage(SelfDestructingMessageCompose.this, receiverRegID, MessageTypes.SELF_DESTRUCTING_MESSAGE, messageStr);
                    }

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        pd = new ProgressDialog(SelfDestructingMessageCompose.this);
                        pd.setMessage(getString(R.string.sending));
                        pd.show();
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        super.onPostExecute(o);
                        if (o != null) {
                            pd.dismiss();
                            SelfDestructingMessageCompose.this.finish();
                        } else {
                            Toast.makeText(SelfDestructingMessageCompose.this, R.string.message_not_sent, Toast.LENGTH_SHORT).show();
                        }
                    }
                }.execute(null, null, null);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sefl_destructing_message_compose, menu);
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