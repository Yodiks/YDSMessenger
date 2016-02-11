package com.yds.messenger.utils;

import android.app.Activity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Created by yds on 30/11/15.
 */

/**
 * Class that has a method to check if Google Play Services is available
 */
public class CheckPlayServices {
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * Check if Google Pay Services are available
     * @param activity
     * @return
     */
    public static boolean check(Activity activity) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                return false;
            }
        }
        return true;
    }
}
/**
 * Created by yds on 30/11/15.
 */