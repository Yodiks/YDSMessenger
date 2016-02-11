package com.yds.messenger.utils;

import android.content.Context;

import com.yds.messenger.R;

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
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yds on 30/11/15.
 */

/**
 * Class that has method to dispatch a message of differing types.
 * Does not work asynchronously.
 */
public class MessageDispatcher {
    /**
     * Method to dispatch a message synchronously
     * @param context - The context of the activity sending the message
     * @param regId - Registration ID of the intended receiver
     * @param messageType - Type of message. Refer MessageTypes class
     * @param message - The message to be sent
     * @return - Response returned by the server
     */
    public static String dispatchMessage(Context context, String regId, int messageType, String message) {
        String result = null;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("https://android.googleapis.com/gcm/send");
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            httpPost.addHeader("Authorization", "key=" + context.getString(R.string.api_key));
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            JSONObject data = new JSONObject();
            data.put("messageType", messageType);
            data.put("messageText", message);
            /**
             * Data items required by the GCM server
             */
            nameValuePairs.add(new BasicNameValuePair("data", data.toString()));
            nameValuePairs.add(new BasicNameValuePair("registration_id", regId));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            HttpResponse response = client.execute(httpPost);
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, "UTF-8");
        } catch (Exception ie) {
            ie.printStackTrace();
        }
        return result;
    }
}
/**
 * Created by yds on 30/11/15.
 */