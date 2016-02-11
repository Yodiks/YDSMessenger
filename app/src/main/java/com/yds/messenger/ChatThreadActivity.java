package com.yds.messenger;

import android.app.ActionBar;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.yds.messenger.listadapters.ChatThreadAdapter;
import com.yds.messenger.models.Contact;
import com.yds.messenger.models.Message;
import com.yds.messenger.models.MessageTypes;
import com.yds.messenger.persistence.Persistence;
import com.yds.messenger.services.DispatchMessageIntentService;
/**
 * Created by yds on 30/11/15.
 */
/**
 * Screen of a chat thread
 */
public class ChatThreadActivity extends ActionBarActivity {

    public static String secretUsername;
    private static String targetRegistrationID;
    private static String contactName;
    private static String prepopulatedMessage;
    private static Message[] messages;
    private static ListView messagesList;
    private static ChatThreadAdapter cta;
    private static String encKey;

    GCMBroadcastReceiver receiver;
    IntentFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_thread);
        //Secret username of the contact
        secretUsername = getIntent().getStringExtra("secretUsername");
        //If prepolulatedMessage is set, it is autofilled in new message text box
        prepopulatedMessage = getIntent().getStringExtra("prepopulatedMessage");

        /**
         * Get other relevant details about the contact
         */
        Contact contact = Persistence.getUser(this, secretUsername);
        if (contact == null) {
            finish();
        } else {
            targetRegistrationID = contact.registrationID;
            contactName = contact.username;
            encKey = contact.encKey;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        /**
         * Register a broadcast receiver so that notifications are not triggered when chat thread is
         * open
         */
        receiver = new GCMBroadcastReceiver(false, this);
        filter = new IntentFilter();
        filter.addAction("com.google.android.c2dm.intent.RECEIVE");
        filter.setPriority(2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        /**
         * Unregisters the broadcast receiver
         */
        unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat_thread, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_selfDestructinMessage) {
            Intent intent = new Intent(this, SelfDestructingMessageCompose.class);
            intent.putExtra("secretUsername", secretUsername);
            intent.putExtra("username", contactName);
            intent.putExtra("registrationID", targetRegistrationID);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Refresh thread by fetching messages again from the Persistence layer
     */
    public void refreshThread() {
        messages = Persistence.getMessages(this, secretUsername);
        cta = new ChatThreadAdapter(this, messages);
        messagesList.setAdapter(cta);
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_chat_thread, container, false);

            messages = Persistence.getMessages(getActivity(), secretUsername);

            /**
             * Fetch and display current messages
             */
            messagesList = (ListView) rootView.findViewById(R.id.listView_chatthread);
            cta = new ChatThreadAdapter(getActivity(), messages);
            messagesList.setAdapter(cta);

            final EditText newMessageField = (EditText) rootView.findViewById(R.id.editText_newMessageText);
            final ImageButton newMessageSendButton = (ImageButton) rootView.findViewById(R.id.button_newMessageSend);

            /**
             * If prepopulatedMessage is set, populate the new message textbox with the message
             */
            if (prepopulatedMessage != null) {
                newMessageField.setText(prepopulatedMessage);
            }

            /**
             * Send the message when button is clicked
             */
            newMessageSendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String messageText = newMessageField.getText().toString().trim();
                    if (messageText.equals("")) {
                        return;
                    }
                    if (messageText.length() > 1000) {
                        Toast.makeText(getActivity(), R.string.message_length_exceeded, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    final String mySecretUsername = Persistence.getSecretUsername(getActivity());
                    final String toRegId = targetRegistrationID;

                    /**
                     * Create intent to trigger a service to send the message
                     */
                    Message newMessage = new Message();
                    newMessage.timestamp = -1l;
                    newMessage.receivedMessage = false;
                    newMessage.messageText = messageText;
                    Persistence.addMessageToQueue(getActivity(), secretUsername, newMessage);
                    Intent intent = new Intent(getActivity(), DispatchMessageIntentService.class);
                    intent.putExtra("registrationID", toRegId);
                    intent.putExtra("messageType", MessageTypes.MORNAL_MESSAGE);
                    intent.putExtra("targetSecretUsername", secretUsername);
                    if (encKey != null) {
                        intent.putExtra("encKey", encKey);
                    }
                    getActivity().startService(intent);

                    /**
                     * Clear the message field
                     */
                    newMessageField.setText("");

                    //Refresh the thread
                    messages = Persistence.getMessages(getActivity(), secretUsername);
                    cta = new ChatThreadAdapter(getActivity(), messages);
                    messagesList.setAdapter(cta);

                }
            });

            ActionBar ab = getActivity().getActionBar();
            ab.setTitle(contactName);
            if (encKey == null) {
                ab.setSubtitle(R.string.not_encrypted);
            } else {
                ab.setSubtitle(R.string.encrypted);
            }
            return rootView;
        }
    }
}
/**
 * Created by yds on 30/11/15.
 */