package com.yds.messenger.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.yds.messenger.R;

import java.util.UUID;

/**
 * Created by yds on 30/11/15.
 */

/**
 * General purpose class to trigger a notification
 */
public class SimpleNotificationMaker {
    /**
     * Method to trigger a notification. Replaces any previous notification.
     * @param context
     * @param title - Title of the notification
     * @param msg - Text of the notification
     * @param contentIntent - PendingIntent to be called when notification is clicked
     */
    public static void sendNotification(Context context, String title, String msg, PendingIntent contentIntent) {
        sendNotification(context, title, msg, contentIntent, false);
    }

    /**
     * Method to trigger a notification. Does not replace previous notifications.
     * @param context
     * @param title - Title of the notification
     * @param msg - Text of the notification
     * @param contentIntent - PendingIntent to be called when notification is clicked
     */
    public static void sendNotification(Context context, String title, String msg, PendingIntent contentIntent, boolean unique) {
        int NOTIFICATION_ID = 0;
        if (unique) {
            NOTIFICATION_ID = UUID.randomUUID().hashCode();
        }
        NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg)
                        .setAutoCancel(true)
                        .setLights(0xffff0000, 300, 10000)
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
/**
 * Created by yds on 30/11/15.
 */