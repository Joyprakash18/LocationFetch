package com.example.locationfetch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import static android.content.Context.MODE_PRIVATE;

public class GPSSwitchRelated {

    public static void onGpsSwitchDisable(final Context context, boolean shouldSendGPSStatusToServer) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("gpsSwitch", "OFF");
        editor.apply();
        stopGPSService(context);
        if (shouldSendGPSStatusToServer) {
            if (LocationFetchHelper.isMyServiceRunning(context, ForegroundLocationService.class)) {
//                sentGPSStatusToServer(context, null, "OFF");
            }
        }
    }

    public static void onGpsSwitchDisableFromSplashScreen(Context context) {
        Log.e("ssssssssssssssss", "onGpsSwitchDisableFromSplashScreen: ");
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("gpsSwitch", "OFF");
        editor.apply();
//        sentGPSUnexpectedOFFStatusToServer(context, "OFF");
    }

    private static void stopGPSService(Context context) {
        Log.d("ffffffffffffffffff", "stopGPSService: ");
        if (LocationFetchHelper.isMyServiceRunning(context, ForegroundLocationService.class)) {
            context.stopService(new Intent(context, ForegroundLocationService.class));
            ForegroundLocationService.onStopLocationService();
            Log.d("ffffffffffffffffff", "isMyServiceRunning: ");
        }
    }

    public static void onGpsSwitchEnable(Context context, Location location, boolean shouldSendGPSStatusToServer) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("gpsSwitch", "ON");
        editor.apply();
        if (shouldSendGPSStatusToServer) {
            sentGPSStatusToServer(context, location, "ON");
        }
    }

    private static void sentGPSStatusToServer(Context context, Location location, String on) {
        String lat = String.valueOf(location.getLatitude());
        String lon = String.valueOf(location.getLongitude());
        Toast.makeText(context,"lat: "+lat+"lon: "+lon,Toast.LENGTH_SHORT).show();

    }

    public static void cancelAlarmManager(Context context) {
//        Intent cancelAlarm=new Intent(context, AlarmBroadcastReceiver.class);
//        PendingIntent cancelAlarmPendingIntent = PendingIntent.getBroadcast(context, LocationFetchHelper.LocationFetchService.REQUEST_CODE_ALARM, cancelAlarm, 0);
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        if (alarmManager != null) {
//            if (cancelAlarmPendingIntent != null) {
//                alarmManager.cancel(cancelAlarmPendingIntent);
//            }
//        }
    }

    public static boolean isGpsSwitchEnabled(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        String gpsSwitch = sharedPreferences.getString("gpsSwitch", "");
        if (gpsSwitch.equalsIgnoreCase("ON")) {
            return true;
        } else if (gpsSwitch.equalsIgnoreCase("OFF")) {
            return false;
        }
        return false;
    }
}
