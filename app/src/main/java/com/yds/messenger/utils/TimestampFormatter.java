package com.yds.messenger.utils;

import android.text.format.DateUtils;

/**
 * Created by yds on 30/11/15.
 */

/**
 * Class to format the timestamp displayed on messages
 */
public class TimestampFormatter {
    /**
     * Convert from a regular timestamp to a String with relative time.
     * Eg: 1 sec ago, 1 day ago, etc.
     * @param timestamp - The time stamp to convert
     * @return
     */
    public static String getAppropriateFormat(long timestamp) {
        String str = "";
        long now = System.currentTimeMillis();
        str = "" + DateUtils.getRelativeTimeSpanString(timestamp, now, 0);
        return str;
    }
}
/**
 * Created by yds on 30/11/15.
 */