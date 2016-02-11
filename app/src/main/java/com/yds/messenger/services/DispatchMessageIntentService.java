package com.yds.messenger.services;

import android.app.IntentService;
import android.content.Intent;

import com.yds.messenger.R;
import com.yds.messenger.models.Message;
import com.yds.messenger.models.MessageTypes;
import com.yds.messenger.persistence.Persistence;
import com.yds.messenger.utils.crypto.EncryptionUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yds on 30/11/15.
 */
/**
 * Service that is invoked to send a regular chat message.
 * It picks up unsent messages form the dispatch queue and sends it.
 * Tries 3 times in case of network problems.
 */
public class DispatchMessageIntentService extends IntentService {

    public DispatchMessageIntentService() {
        super("DispatchMessageIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String regId = intent.getStringExtra("registrationID");
            String secretUsername = intent.getStringExtra("targetSecretUsername");
            String encKey = intent.getStringExtra("encKey");

            /**
             * Pick up next message from the queue of unsent messages.
             */
            Message toBeSent = Persistence.getMessageFromQueue(this, secretUsername);

            /**
             * Construct the object representing the message to be sent
             */
            JSONObject messageJSON = new JSONObject();
            String messageText = "";
            try {
                messageJSON.put("secretUsername", Persistence.getSecretUsername(this));
                String finalMessageText = null;
                /**
                 * If the encKey is not null, encrypt the message text
                 */
                if (encKey != null) {
                    finalMessageText = EncryptionUtils.encrypt(toBeSent.messageText, encKey);
                } else {
                    finalMessageText = toBeSent.messageText;
                }

                messageJSON.put("messageText", finalMessageText);
                messageText = messageJSON.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String result = null;
            int retries = 3; //Number of times to try in case of network error

            while (result == null && retries > 0) {
                try {
                    if (retries == 2) {
                        Thread.sleep(3000);
                    } else if (retries == 1) {
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    HttpClient client = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost("https://android.googleapis.com/gcm/send");
                    httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                    httpPost.addHeader("Authorization", "key=" + getString(R.string.api_key));
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                    JSONObject data = new JSONObject();
                    data.put("messageType", MessageTypes.MORNAL_MESSAGE);
                    data.put("messageText", messageText);
                    nameValuePairs.add(new BasicNameValuePair("data", data.toString()));
                    nameValuePairs.add(new BasicNameValuePair("registration_id", regId));
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
                    HttpResponse response = client.execute(httpPost);
                    HttpEntity entity = response.getEntity();
                    result = EntityUtils.toString(entity, "UTF-8");
                } catch (Exception ie) {
                    ie.printStackTrace();
                    result = null;
                }
                retries--;
            }

            if (result == null) {
                //Message failed to send. 0 in timestamp indicates this to ChatThreadAdaptor
                toBeSent.timestamp = 0l;
            } else {
                //Sent successfully
                toBeSent.timestamp = System.currentTimeMillis();
            }

            //Add message to actual list of messages for this contact
            //Add message to actual list of messages for this contact
            Persistence.addMessage(this, secretUsername, toBeSent);
            //Remove message from the unsent queue
            Persistence.removeMessageFromQueue(this, secretUsername);
        }
    }
}
/**
 * Created by yds on 30/11/15.
 */