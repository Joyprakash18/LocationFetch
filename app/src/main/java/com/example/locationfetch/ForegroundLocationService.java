package com.example.locationfetch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import static android.widget.Toast.makeText;

public class ForegroundLocationService extends Service implements GoogleApiClient.ConnectionCallbacks, LocationListener {

    public static final CharSequence NOTIFICATION_CHANNEL_NAME = "eve.GPS.Foreground";
    private static final Uri DEFAULT_SOUND_URI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    private static final int REQUEST_CHECK_SETTINGS = 2002;
    private static final int NOTIFICATION_ID = 1387;
    private static final int BATTERY_SAVER_NOTIFICATION_ID = 1387;
    private LocationManager locationManager;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private PowerManager powerManager;
    private NotificationManager mNotifyMgr;
    private static boolean isServiceAlive = false;

    public ForegroundLocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = null;
        final String notificationChannelId = "location_fetch_notification";

        //main service notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("pending_intent", true);
        notificationIntent.putExtra("fromPage", 1);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        final Intent stopIntent = new Intent(this, FinishActivity.class);
        stopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        final PendingIntent stopPendingIntent = PendingIntent.getActivity(this, 1, stopIntent, PendingIntent.FLAG_ONE_SHOT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // only for Oreo
            notification = createSimpleNotificationForOreo(this, pendingIntent, stopPendingIntent, "PARTNER", "Fetching your location", notificationChannelId);
        } else {
            notification = createSimpleNotification(this, pendingIntent, stopPendingIntent, "PARTNER", "Fetching your location");
        }
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
//        notificationBuilder
//                .setContentTitle("EVE")
//                .setContentText("Fetching your location")
//                .setSmallIcon(R.drawable.ic_small_notification)
//                .addAction(R.drawable.ic_location_service_stop, "STOP", stopPendingIntent)
//                .setDeleteIntent(stopPendingIntent)
//                .setContentIntent(pendingIntent);
//        /*final */Notification notification = notificationBuilder.build();
//        final Handler pendingIntentHandler = new Handler();
//        pendingIntentHandler.postDelayed(new Runnable() {
//            NotificationManager nmgr = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
//            @Override
//            public void run() {
//                if (isForeground("com.gpsinhand.gps_in_hand")) {
//                        nmgr.cancel(1337);
////                        notificationBuilder.mActions.clear();
//                }else {
//                    nmgr.notify(1337, notification);
////                    notificationBuilder.addAction(R.drawable.ic_location_service_stop, "STOP", stopPendingIntent);
//                }
//                pendingIntentHandler.postDelayed(this,2000);
//            }
//        }, 2000);

        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE/*.FLAG_ONGOING_EVENT*/;
//        Intent i = new Intent(this, RouteActivity.class);
//
//        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
//                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
//
//        notification.setLatestEventInfo(this, "Notification title",
//                "notification message)", pi);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel mChannel = new NotificationChannel(notificationChannelId, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
            }
        }
        startForeground(NOTIFICATION_ID, notification);
        // Build the Play services client for use by the Fused Location Provider and the Places API.
        // Use the addApi() method to request the Google Places API and the Fused Location Provider.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "LocationFetch:MyWakelockTag");
        wakeLock.acquire();
        return (START_STICKY);
    }

    //This method is for before Oreo
    public static Notification createSimpleNotification(Context context, PendingIntent pendingIntent, PendingIntent stopPendingIntent, String notificationTitle, String notificationContent){
        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_location_on_white)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_location_on_white))
                .setSound(DEFAULT_SOUND_URI)
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationContent))
                .addAction(R.drawable.ic_location_service_stop, "STOP", stopPendingIntent)
                .setDeleteIntent(stopPendingIntent)
                .setContentIntent(pendingIntent)
                .build();
    }

    //This method is for Oreo
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Notification createSimpleNotificationForOreo(Context context, PendingIntent pendingIntent, PendingIntent stopPendingIntent, String notificationTitle, String notificationContent, String notificationChannelId) {
        Notification.BigTextStyle bigText = new Notification.BigTextStyle();
        bigText.bigText(notificationContent);
        bigText.setBigContentTitle(notificationTitle);
        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_location_on_white)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_location_on_white))
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setStyle(bigText)
                .setChannelId(notificationChannelId)
                .addAction(R.drawable.ic_location_service_stop, "STOP", stopPendingIntent)
                .setDeleteIntent(stopPendingIntent)
                .setContentIntent(pendingIntent);

        Notification notification = notificationBuilder.build();
//        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        return notification;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startListenLocation();

//        this is used if we want to get location directly without using onLocationChanged method

//        Location mLastKnownLocation = LocationServices.FusedLocationApi
//                .getLastLocation(mGoogleApiClient);
//        // Loading Data for Map and Updating User Coordinate
//        CordinateServiceManager controller = CordinateServiceManager.getInstance(getApplicationContext());
//        controller.serverCordinateUpdater(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), mLastKnownLocation.getAccuracy(), mLastKnownLocation.getTime());
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public void startListenLocation() {
        createLocationRequest();
        startLocationUpdates();
    }


    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(7000);
        mLocationRequest.setFastestInterval(4000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates() {
        isServiceAlive = true;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            handleLocationRequestPermission();
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @SuppressLint("MissingPermission")
    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isServiceAlive) {
            if (!InternetGpsConnectionChecker.isGpsEnabled(getApplicationContext())) {
//            Toast.makeText(LocationService.this, "Gps in Hand cannot get current location information!", Toast.LENGTH_SHORT).show();
//            handleLocationRequestPermission();
            }
            try {
                if (!InternetGpsConnectionChecker.isNetworkAvailable(getApplicationContext())) {
//                makeText(this, "Gps in Hand cannot connect to the Internet!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
//            new SendExceptionToServer(getApplicationContext(), e);
            }

            if (!LocationFetchHelper.isForeground(this, "com.example.locationfetch")) {
//            NotificationCompat.Builder mBuilder =
//                    new NotificationCompat.Builder(this)
//                            .setSmallIcon(R.drawable.ic_small_notification)
//                            .setContentTitle("Battery Saver ON")
//                            .setContentText("May not fetch your location properly");
                powerManager = (PowerManager)
                        getSystemService(Context.POWER_SERVICE);
//                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
//                        && powerManager.isPowerSaveMode()) {
//                    NotificationHelper.sendNotification(this, BATTERY_SAVER_NOTIFICATION_ID, null, "Battery saver ON! May not fetch your location properly");
//                } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
//                        && !powerManager.isPowerSaveMode()) {
//                    NotificationHelper.clearNotification(this, BATTERY_SAVER_NOTIFICATION_ID);
//
//                }
            }
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            handleLocationRequestPermission();
            }
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            Toast.makeText(this, "GPS not enabled", Toast.LENGTH_SHORT).show();
//            handleLocationRequestPermission();
            }
//        if (location.getAccuracy()>=40.0f || location.getAccuracy()==0.0f ){
//            return;
//        }
//        CordinateServiceManager controller = CordinateServiceManager.getInstance(getApplicationContext());
//        controller.serverCordinateUpdater(new LatLng(location.getLatitude(), location.getLongitude()), location.getAccuracy(),location.getTime());
            LocationFetchHelper.sentCurrentLocationToServer(getApplicationContext(), location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getTime());
        }else {
            stopLocationUpdates();
        }
    }

    private class RequestLocationUIActivity extends AppCompatActivity {

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PendingIntent pI = (getIntent().getParcelableExtra("resolution"));
            try {
                startIntentSenderForResult(pI.getIntentSender(),REQUEST_CHECK_SETTINGS,null,10,10,10);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
//            pI.getIntentSender().startResolutionForResult(
//                    RequestLocationUIActivity.this,
//                    REQUEST_CHECK_SETTINGS);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
            switch (requestCode) {
                case REQUEST_CHECK_SETTINGS:
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            makeText(this, "Location ON", Toast.LENGTH_SHORT).show();
                            // All required changes were successfully made
                            break;
                        case Activity.RESULT_CANCELED:
                            makeText(this, "Location OFF", Toast.LENGTH_SHORT).show();
                            // The user was asked to change settings, but chose not to
                            break;
                        default:
                            break;
                    }
                    break;
            }
        }

//    public void startListenLocation() {
//        locationManager = (LocationManager) getApplicationContext()
//                .getSystemService(Context.LOCATION_SERVICE);
//        locationListener = new LocationListener() {
//            public void onLocationChanged(Location location) {
//                CurrentRouteManager.getSharedManager().updateWithLocation(
//                        location);
//            }
//
//            public void onStatusChanged(String provider, int status,
//                                        Bundle extras) {
//
//            }
//
//            public void onProviderEnabled(String provider) {
//
//            }
//
//            public void onProviderDisabled(String provider) {
//
//            }
//
//        };
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
//                200, locationListener);
//        locationManager.requestLocationUpdates(
//                LocationManager.NETWORK_PROVIDER, 0, 200, locationListener);
    }
    private void handleLocationRequestPermission(){
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
//        builder.addLocationRequest(mLocationRequest);
//        Task<LocationSettingsResponse> task =
//                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());
//
//        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
//            @Override
//            public void onComplete(Task<LocationSettingsResponse> task) {
//                try {
//                    LocationSettingsResponse response = task.getResult(ApiException.class);
//                    // All location settings are satisfied. The client can initialize location
//                    // requests here.
//                } catch (ApiException exception) {
//                    switch (exception.getStatusCode()) {
//                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                            // Location settings are not satisfied. But could be fixed by showing the
//                            // user a dialog.
//                            try {
//                                // Cast to a resolvable exception.
//                                ResolvableApiException resolvable = (ResolvableApiException) exception;
//                                // Show the dialog by calling startResolutionForResult(),
//                                // and check the result in onActivityResult().
//                                PendingIntent pI = resolvable.getResolution();
//                                mGoogleApiClient.getContext().startActivity(new Intent(mGoogleApiClient.getContext(), RequestLocationUIActivity.class)
//                                        .putExtra("resolution", pI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
//                            } catch (ClassCastException e) {
//                                // Ignore, should be an impossible error.
//                            }
//                            break;
//                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//                            // Location settings are not satisfied. However, we have no way to fix the
//                            // settings so we won't show the dialog.
//                            break;
//                    }
//                }
//            }
//        });
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Location OFF")
                .setMessage("Please turn On your location")
                .create();

        try {
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            alertDialog.show();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        stopSelf();
        stopForeground(true);
        stopService(new Intent(this, ForegroundLocationService.class));
        ForegroundLocationService.onStopLocationService();
    }

    public static void onStopLocationService(){
        isServiceAlive = false;
    }
}
