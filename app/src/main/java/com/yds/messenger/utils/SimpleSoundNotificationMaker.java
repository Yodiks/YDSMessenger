package com.yds.messenger.utils;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

/**
 * Created by yds on 30/11/15.
 */
public class SimpleSoundNotificationMaker {
    public static void sendNotification(Context context) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context, notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
/**
 * Created by yds on 30/11/15.
 */