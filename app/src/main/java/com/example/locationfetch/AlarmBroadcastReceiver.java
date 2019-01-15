package com.example.locationfetch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;

public class AlarmBroadcastReceiver extends BroadcastReceiver {

    private static final int REQUEST_CODE_ALARM = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
//        FetchLocationSuccessListener listener = LocationFetchHelperSingleton.getInstance().getFetchLocationListener();
//        FetchLocationFailureListener failureListener = LocationFetchHelperSingleton.getInstance().getFetchLocationFailureListener();
        FetchLocationSuccessListener listener = null;
        FetchLocationFailureListener failureListener = null;
        long locationIntervalTime = LocationFetchHelperSingleton.getInstance().getLocationIntervalTime();
        long fastestIntervalTime = LocationFetchHelperSingleton.getInstance().getLocationFastestIntervalTime();
        int locationPriority = LocationFetchHelperSingleton.getInstance().getLocationPriority();
        if (locationIntervalTime == 0) {
            new LocationFetchHelper(context, listener, failureListener, LocationFetchHelper.INTERVAL_TIME, LocationFetchHelper.FASTEST_INTERVAL_TIME, PRIORITY_HIGH_ACCURACY, true);
        }else {
            new LocationFetchHelper(context, listener, failureListener, locationIntervalTime, fastestIntervalTime, locationPriority, true);
        }
    }
}
