package com.example.locationfetch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.MODE_PRIVATE;

/**
 * Generic plain java class which will help to fetch location by using Activity or Service & alarm
 * manager & alarm broadcast & on boot complete broadcast etc.
 * Add the following line of code in the manifest under the application tag
 * <p> </br>
 * &lt;activity
 * android:name=".locationfetchrelated.LocationFetchHelper$LocationFetchActivity"
 * android:theme="@style/Theme.AppCompat.Translucent" /&gt;
 * </p>
 * <p>
 * &lt;service
 * android:name=".locationfetchrelated.LocationFetchHelper$LocationFetchService"
 * android:enabled="true"
 * android:exported="true" /&gt;
 * </p>
 * <p>
 * &lt;receiver
 * android:name=".locationfetchrelated.AlarmBootReceiver"
 * android:enabled="true"
 * android:exported="true">
 * &lt;intent-filter>
 * &lt;action android:name="android.intent.action.BOOT_COMPLETED"/&gt;
 * &lt;/intent-filter>
 * &lt;/receiver>
 * </p>
 * <p>
 * &lt;receiver
 * android:name=".locationfetchrelated.AlarmBroadcastReceiver"
 * android:enabled="true"
 * android:exported="true" /&gt;
 * </p>
 */
public class LocationFetchHelper {
    private Context context;
    public static final long INTERVAL_TIME = 20 * 1000;
    public static final long FASTEST_INTERVAL_TIME = 10 * 1000;

    /**
     * Use this constructor for default operations (1 minute, balanced power)
     * <p>
     *
     * @param context          use Activity context
     *                         </p><p>
     * @param mListener        listener for getting location fetch callbacks (success or failed) {@link FetchLocationSuccessListener}
     *                         </p><p>
     * @param mFailureListener failure listener for getting error message callback
     *                         </p><p>
     * @param shouldUseService if true, this helper class will be using a service and alarm manager
     *                         for fetching continuous and repeated location fetching; even after
     *                         rebooting it may continue
     *                         </p><p>
     * @param continueFetchingLocation send true as an argument if you want to continue fetching
     *                                 the location after initial location fetch. (For now it is not working)
     *                         </p>
     *
     */
    public LocationFetchHelper(Context context, FetchLocationSuccessListener mListener, FetchLocationFailureListener mFailureListener, boolean shouldUseService, boolean continueFetchingLocation) {
        this.context = context;
        LocationFetchHelperSingleton.getInstance().setFetchLocationSuccessListener(mListener);
        LocationFetchHelperSingleton.getInstance().setFetchLocationFailureListener(mFailureListener);
        LocationFetchHelperSingleton.getInstance().setLocationIntervalTime(INTERVAL_TIME);
        LocationFetchHelperSingleton.getInstance().setLocationFastestIntervalTime(FASTEST_INTERVAL_TIME);
        LocationFetchHelperSingleton.getInstance().setShouldUseService(shouldUseService);
        LocationFetchHelperSingleton.getInstance().setContinueFetchingLocation(continueFetchingLocation);
        LocationFetchHelperSingleton.getInstance().setLocationPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationFetchHelperSingleton.getInstance().setIsOnlyPermissionCheck(false);
        startLocationFetchActivity();
    }

    /**
     * Use this constructor for extended operations
     * <p>
     *
     * @param context                     use Activity context
     *
     *                                    </p><p>
     * @param mListener                   listener for getting location fetch callbacks (success or failed)  {@link FetchLocationSuccessListener}
     *                                    </p><p>
     * @param mFailureListener            failure listener for getting error message callback
     *                                    </p><p>
     * @param locationIntervalTime        the interval time for fetching location
     *                                    </p><p>
     * @param locationFastestIntervalTime the fastest interval time for fetching location,
     *                                    this <b>will be ignored if param shouldUseService is true<b/>
     *                                    </p><p>
     * @param locationPriority            the location priority for fetching location;
     *                                    must be one of the<br/>
     *                                    {@link LocationRequest#PRIORITY_HIGH_ACCURACY},<br/>
     *                                    {@link LocationRequest#PRIORITY_BALANCED_POWER_ACCURACY},<br/>
     *                                    {@link LocationRequest#PRIORITY_LOW_POWER},<br/>
     *                                    {@link LocationRequest#PRIORITY_NO_POWER}<br/>
     *                                    </p><p>
     * @param shouldUseService            if true, this helper class will be using a service and alarm manager
     *                                    for fetching continuous and repeated location fetching; even after
     *                                    rebooting it may continue
     *                                    </p>
     */
    public LocationFetchHelper(Context context, FetchLocationSuccessListener mListener, FetchLocationFailureListener mFailureListener, long locationIntervalTime, long locationFastestIntervalTime, int locationPriority, boolean shouldUseService) {
        this.context = context;
        LocationFetchHelperSingleton.getInstance().setFetchLocationSuccessListener(mListener);
        LocationFetchHelperSingleton.getInstance().setFetchLocationFailureListener(mFailureListener);
        LocationFetchHelperSingleton.getInstance().setLocationIntervalTime(locationIntervalTime);
        LocationFetchHelperSingleton.getInstance().setLocationFastestIntervalTime(locationFastestIntervalTime);
        LocationFetchHelperSingleton.getInstance().setShouldUseService(shouldUseService);
        LocationFetchHelperSingleton.getInstance().setLocationPriority(locationPriority);
        LocationFetchHelperSingleton.getInstance().setIsOnlyPermissionCheck(false);
        startLocationFetchActivity();
    }

    /**
     * Use this constructor for default operations (1 minute, balanced power)
     * <p>
     *
     * @param context                    use Activity context
     *                                   </p><p>
     * @param locationRequest
     * @param locationPermissionListener listener for getting location permission callbacks (success or failed) {@link LocationPermissionListener}
     */
    public LocationFetchHelper(Context context, LocationRequest locationRequest, LocationPermissionListener locationPermissionListener) {
        this.context = context;
        LocationFetchHelperSingleton.getInstance().setLocationPermissionListener(locationPermissionListener);
        LocationFetchHelperSingleton.getInstance().setIsOnlyPermissionCheck(true);
        LocationFetchHelperSingleton.getInstance().setLocationRequest(locationRequest);
        startLocationFetchActivity();
    }

    /**
     * Call this method to stop the continuous location update
     *
     * @param context Activity context
     * @return true if successfully canceled the service, false if failed
     */
    public static boolean stopLocationService(Context context) {
//        Intent cancelAlarm = new Intent(context, AlarmBroadcastReceiver.class);
//        PendingIntent cancelAlarmPendingIntent = PendingIntent.getBroadcast(context, LocationFetchHelper.LocationFetchService.REQUEST_CODE_ALARM, cancelAlarm, 0);
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        if (alarmManager != null) {
//            alarmManager.cancel(cancelAlarmPendingIntent);
//            return true;
//        } else {
//            return false;
//        }
        return false;
    }

    public static void sentCurrentLocationToServer(Context context, double latitude, double longitude, float accuracy, long time) {
        if (context == null) {
            context = LocationFetchHelperSingleton.getInstance().getContext();
        }
        if (context != null) {
            String lat = String.valueOf(latitude);
            String lon = String.valueOf(longitude);
            Toast.makeText(context, "lat: "+lat+"lon: "+lon, Toast.LENGTH_SHORT).show();
        }
    }

    private void startLocationFetchActivity() {
        try {
            Activity activity = (Activity) context;
            Intent intent = new Intent(activity, LocationFetchActivity.class);
            activity.startActivity(intent);
        } catch (Exception e) {
            //this is a call from the alarm broadcast
            //start service here
            startLocationFetchService(context);
        }
    }

    private static void startLocationFetchService(Context context) {
        if (context != null) {
            LocationFetchHelperSingleton.getInstance().setContext(context);
            try {
                Intent intent = new Intent(context.getApplicationContext(), ForegroundLocationService.class);
                if (!isMyServiceRunning(context, ForegroundLocationService.class)) {
                    context.startService(intent);
                }
//            Intent intentService = new Intent(context, LocationFetchService.class);
//            context.stopService(intentService);
//            context.startService(intentService);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class LocationFetchActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener {

        private static final int REQUEST_FOR_LOCATION = 10123;
        private static final int REQUEST_CHECK_SETTINGS = 20123;
        private FusedLocationProviderClient mFusedLocationClient;
        private LocationRequest mLocationRequest;
        private FetchLocationSuccessListener mListener;
        private FetchLocationFailureListener mfailureListener;
        private boolean mRequestingLocationUpdates = false;
        private LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                afterLocationFetchSucceed(locationResult.getLastLocation());
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
                if (!locationAvailability.isLocationAvailable()) {
                    afterLocationFetchFailed("Unable to fetch your location");
                }
            }
        };
        private GoogleApiClient mGoogleApiClient;

        //override method if you are using LocationListener. Else, simple method for next work
        @SuppressLint("MissingPermission")
        public void onLocationChanged(Location location) {
            afterLocationFetchSucceed(location);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopLocationUpdates();
                }
            }, 150);
        }

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setUpGoogleApiClient();
        }

        @Override
        protected void onResume() {
            super.onResume();
            if (mRequestingLocationUpdates) {
                startLocationUpdates();
            }
        }

        @Override
        protected void onPause() {
            super.onPause();
            try {
                if (mGoogleApiClient.isConnected()) {
                    if (mRequestingLocationUpdates) {
                        stopLocationUpdates();
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        private void initiateLocationRequest() {
            if (LocationFetchHelperSingleton.getInstance().getIsOnlyPermissionCheck()) {
                requestPermissionForLocation();
            } else {
                mListener = LocationFetchHelperSingleton.getInstance().getFetchLocationSuccessListener();
                mfailureListener = LocationFetchHelperSingleton.getInstance().getFetchLocationFailureListener();
                initializeFusedLocationProviderClient();
                createLocationRequest();
                requestPermissionForLocation();
            }
        }

        private void initializeFusedLocationProviderClient() {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }

        private void setUpGoogleApiClient() {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
//                .addApi(Places.GEO_DATA_API)
//                .addApi(Places.PLACE_DETECTION_API)
                    .build();
            mGoogleApiClient.connect();
        }

        //first call this method
        protected void createLocationRequest() {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(LocationFetchHelperSingleton.getInstance().getLocationIntervalTime());
            mLocationRequest.setFastestInterval(LocationFetchHelperSingleton.getInstance().getLocationFastestIntervalTime());
            mLocationRequest.setPriority(LocationFetchHelperSingleton.getInstance().getLocationPriority());
        }

        //then this method
        public void requestPermissionForLocation() {
            Context context = LocationFetchHelperSingleton.getInstance().getContext();
            if (context == null) {
                context = getApplicationContext();
            }
            if (context != null) {
                if (ContextCompat.checkSelfPermission(context.getApplicationContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context.getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    // not granted, explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_FOR_LOCATION);
                    } else {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_FOR_LOCATION);
                    }
                } else {
                    //permission already granted
                    handleLocationRequestPermission();
                }
            } else {
                afterLocationFetchFailed("Context not found! Please try again later.");
            }
        }

        //permission granted
        private void handleLocationRequestPermission() {
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
            if (LocationFetchHelperSingleton.getInstance().getIsOnlyPermissionCheck()) {
                builder.addLocationRequest(LocationFetchHelperSingleton.getInstance().getLocationRequest());
            } else {
                builder.addLocationRequest(mLocationRequest);
            }
            Task<LocationSettingsResponse> task =
                    LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

            task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                @Override
                public void onComplete(Task<LocationSettingsResponse> task) {
                    try {
                        LocationSettingsResponse response = task.getResult(ApiException.class);
                        // requests here.
                        // All location settings are satisfied. The client can initialize location
                        if (LocationFetchHelperSingleton.getInstance().getIsOnlyPermissionCheck()) {
                            LocationFetchHelperSingleton.getInstance().getLocationPermissionListener().onPermissionGranted();
                            finish();
                        } else {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    getCurrentLocation();
                                }
                            }, 3000);
                        }
                    } catch (ApiException exception) {
                        int code = exception.getStatusCode();
                        switch (code) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied. But could be fixed by showing the
                                // user a progressDialog.
                                try {
                                    // Cast to a resolvable exception.
                                    ResolvableApiException resolvable = (ResolvableApiException) exception;
                                    // Show the progressDialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    resolvable.startResolutionForResult(
                                            LocationFetchActivity.this,
                                            REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException e) {
                                    // Ignore the error.
                                } catch (ClassCastException e) {
                                    // Ignore, should be an impossible error.
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                // Location settings are not satisfied. However, we have no way to fix the
                                // settings so we won't show the progressDialog.
                                new android.app.AlertDialog.Builder(LocationFetchActivity.this)
                                        .setMessage("GPS is not enabled. Do you want to go to settings menu?")
                                        .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                                startActivity(intent);
                                            }
                                        })
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (LocationFetchHelperSingleton.getInstance().getIsOnlyPermissionCheck()) {
                                                    LocationFetchHelperSingleton.getInstance().getLocationPermissionListener().onPermissionDenied("Location denied");
                                                } else {
                                                    afterLocationFetchFailed("Location denied");
                                                }
                                            }
                                        })
                                        .setCancelable(false)
                                        .show();
                                break;
                        }
                    }
                }
            });
        }

        @SuppressLint("MissingPermission")
        private void startLocationUpdates() {
            /*uncomment this line of code if you want to use onLocationChange of LocationListener class.
             * also uncomment the implements interface of LocationListener in this activity
             */
            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
//            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
//                    mLocationCallback,
//                    Looper.myLooper() /* Looper */);
//            mRequestingLocationUpdates = true;
            } catch (SecurityException e) {

                e.printStackTrace();
            }
        }

        @SuppressLint("MissingPermission")
        private void stopLocationUpdates() {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
//            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
//            mRequestingLocationUpdates = true;
        }

        //fetch location
        @SuppressLint("MissingPermission")
        private void getCurrentLocation() {
            if (LocationFetchHelperSingleton.getInstance().isShouldUseService()) {
                if (getApplicationContext() != null)
                    LocationFetchHelper.startLocationFetchService(getApplicationContext());
                else
                    LocationFetchHelper.startLocationFetchService(LocationFetchActivity.this);
                finish();
            } else
                startLocationUpdates();
//                mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new MyOnSuccessListener());
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            initiateLocationRequest();
        }

        @Override
        public void onConnectionSuspended(int i) {
            afterLocationFetchFailed("Could not connect to google location service");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            afterLocationFetchFailed("Could not connect to google location service");
        }

        //location listener
        private class MyOnSuccessListener implements OnSuccessListener<Location> {
            private int mOnSuccessCallCounter;

            @SuppressLint("MissingPermission")
            @Override
            public void onSuccess(Location location) {
                //location fetch successful
                mOnSuccessCallCounter++;
                if (location != null) {
                    //able to get current location
                    afterLocationFetchSucceed(location);
                } else {
                    //location returned is null
                    if (mOnSuccessCallCounter <= 5) {
                        //try another time
                        mFusedLocationClient.getLastLocation().addOnSuccessListener(LocationFetchActivity.this, this);
                    } else {
                        startLocationUpdates();
                    }
                }
            }
        }

        //not called for now, it is not working WIP
        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               @NonNull String permissions[], @NonNull int[] grantResults) {
            Log.d("sayan", " onrequestlocationpermission");
            switch (requestCode) {
                case REQUEST_CHECK_SETTINGS: {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // permission was granted
                        Log.d("sayan", " yes selected");
                        if (LocationFetchHelperSingleton.getInstance().getIsOnlyPermissionCheck()) {
                            LocationFetchHelperSingleton.getInstance().getLocationPermissionListener().onPermissionGranted();
                            finish();
                        } else {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    getCurrentLocation();
                                }
                            }, 3000);
                        }
                    } else {
                        //permission denied
                        afterLocationFetchFailed("Location permission denied");
                    }
                    break;
                }
                case REQUEST_FOR_LOCATION: {
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // permission was granted
//                    Toast.makeText(getApplicationContext(), "SMS Permission granted", Toast.LENGTH_LONG).show();
                        handleLocationRequestPermission();
                    } else {
                        afterLocationFetchFailed("Please turn on location permission in Settings");
                    }
                    break;
                }
            }
        }

        //after permission and location ON, this method will be called
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
            switch (requestCode) {
                case REQUEST_CHECK_SETTINGS: {
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            // All required changes were successfully made
//                            Toast.makeText(this, "Location ON", Toast.LENGTH_SHORT).show();
                            if (LocationFetchHelperSingleton.getInstance().getIsOnlyPermissionCheck()) {
                                LocationFetchHelperSingleton.getInstance().getLocationPermissionListener().onPermissionGranted();
                                finish();
                            } else {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        getCurrentLocation();
                                    }
                                }, 3000);
                            }
                            break;
                        case Activity.RESULT_CANCELED:
                            // The user was asked to change settings, but chose not to
//                            Toast.makeText(this, "Location OFF", Toast.LENGTH_SHORT).show();
                            afterLocationFetchFailed("Please turn on your location in Settings");
                            break;
                        default:
                            break;
                    }
                    break;
                }
            }
        }

        private void afterLocationFetchFailed(final String errorMessage) {
            if (LocationFetchHelperSingleton.getInstance().getIsOnlyPermissionCheck()) {
                LocationFetchHelperSingleton.getInstance().getLocationPermissionListener().onPermissionDenied(errorMessage);
                finish();
            } else {
                if (mfailureListener != null) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mfailureListener.onLocationFetchFailed(errorMessage);
                        }
                    }, 200);
                }
                finish();
            }
        }

        private void afterLocationFetchSucceed(final Location location) {
//            if (location.getAccuracy() > 100.0f){
//                afterLocationFetchFailed("Your location is not accurate enough, please try again later");
//                return;
//            }
            if (mListener != null) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onLocationFetched(location);
                        if (!LocationFetchHelperSingleton.getInstance().isShouldUseService() && LocationFetchHelperSingleton.getInstance().isContinueFetchingLocation()) {
//                            LocationFetchHelper.LocationFetchService.setAlarm(LocationFetchHelper.LocationFetchActivity.this, INTERVAL_TIME);
                        }
                    }
                }, 200);
            }
            stopLocationUpdates();
            finish();
        }

    }

    public static void sentCurrentLocationToServer(Context context, String latitude, String longitude) {
        if (context == null) {
            context = LocationFetchHelperSingleton.getInstance().getContext();
        }
        if (context != null) {
            Toast.makeText(context, "lat: "+latitude+"lon: "+longitude, Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isForeground(Context context, String myPackage) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
            ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
            return componentInfo.getPackageName().equals(myPackage);
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        }
    }
}