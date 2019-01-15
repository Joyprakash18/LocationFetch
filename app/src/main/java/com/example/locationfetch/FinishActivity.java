package com.example.locationfetch;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class FinishActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        Log.d("ffffffffffffffffff", "FinishActivity: ");
        Intent stopIntent = new Intent(this, FinishActivity.class);
        PendingIntent stopPendingIntent = PendingIntent.getActivity(this, 1, stopIntent, 0);
        if (LocationFetchHelper.isMyServiceRunning(this, ForegroundLocationService.class)) {
            GPSSwitchRelated.onGpsSwitchDisable(this, false);
            sentGPSStatusToServer(this, "OFF");
            new Handler().postDelayed(new MyRunnable(this), 10000);
            Log.d("ffffffffffffffffff", "onGpsSwitchDisable: ");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    private static void sentGPSStatusToServer(final Context context, final String gpsSwitchStatus) {
        if (context != null) {
            new LocationFetchHelper(context, new FetchLocationSuccessListener() {
                @Override
                public void onLocationFetched(Location location) {
                    stopAllOperations(context);
                }
            }
                    , new FetchLocationFailureListener() {
                @Override
                public void onLocationFetchFailed(String errorMessage) {
                    Toast.makeText(context, "You must enable your location for this task", Toast.LENGTH_SHORT).show();
                }
            }
                    , false, false);
        }
    }

    private static void stopAllOperations(Context context) {
        try {
            stopGPSService(context);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }, 200);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void stopGPSService(Context context) {
        Log.d("ffffffffffffffffff", "stopGPSService: ");
        if (LocationFetchHelper.isMyServiceRunning(context, ForegroundLocationService.class)) {
            context.stopService(new Intent(context, ForegroundLocationService.class));
            ForegroundLocationService.onStopLocationService();
            Log.d("ffffffffffffffffff", "isMyServiceRunning: ");
        }
    }

    private class MyRunnable implements Runnable {
        private Context context;
        MyRunnable(Context context) {
            this.context = context;
        }
        @Override
        public void run() {
            stopAllOperations(context);
        }
    }
}