package com.yds.messenger.services;

import android.app.IntentService;
import android.content.Intent;

import com.yds.messenger.R;
import com.yds.messenger.models.Contact;
import com.yds.messenger.models.MessageTypes;
import com.yds.messenger.persistence.Persistence;

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
 * Service that inform contacts about change in registration id
 */
public class DispatchRegUpdateMessageIntentService extends IntentService {

    public DispatchRegUpdateMessageIntentService() {
        super("DispatchRegUpdateMessageIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String mySecretUsername = Persistence.getSecretUsername(this);
            String newRegId = Persistence.getRegistrationID(this);

            /**
             * Construct object informing of this change
             */
            JSONObject messageJSON = new JSONObject();
            String messageText = "";
            try {
                messageJSON.put("secretUsername", mySecretUsername);
                messageJSON.put("registrationID", newRegId);
                messageText = messageJSON.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Contact[] contacts = Persistence.getContacts(this);
            for (Contact c : contacts) {
                String result = null;
                int retries = 3; //No of times to try sending

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
                        data.put("messageType", MessageTypes.REGISTRATION_UPDATE);
                        data.put("messageText", messageText);
                        nameValuePairs.add(new BasicNameValuePair("data", data.toString()));
                        nameValuePairs.add(new BasicNameValuePair("registration_id", c.registrationID));
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
                    /**
                     * Sending to this contact failed.
                     * Mark this contact for informing later.
                     */
                    //To be handled
                }
            }
        }
    }
}
/**
 * Created by yds on 30/11/15.
 */