package com.example.locationfetch;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private SwitchCompat gpsSwitch;
    private FrameLayout gpsFrame;
    private boolean isSwitchOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isSwitchOn = false;
        initializeViews();
        onClickListener();
        loadGPSSwitchStatus();
    }

    private void loadGPSSwitchStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        String gpsswitch = sharedPreferences.getString("gpsSwitch", "");
        if (gpsswitch.equalsIgnoreCase("ON")) {
            onSwitchEnabled();
            gpsSwitch.setChecked(true);
            isSwitchOn = true;
        } else if (gpsswitch.equalsIgnoreCase("OFF")) {
            gpsSwitch.setChecked(false);
            isSwitchOn = false;
        }
    }

    private void initializeViews() {
        gpsSwitch = findViewById(R.id.gpsSwitch);
        gpsFrame = findViewById(R.id.gpsFrame);
    }

    private void onClickListener() {
        gpsFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPhoneState();
                if (isSwitchOn) {
                    onSwitchDisabled();
                } else {
                    gpsSwitch.setChecked(true);
                    Toast.makeText(getApplicationContext(), "Fetching your location", Toast.LENGTH_SHORT).show();
                    onSwitchEnabled();
                    isSwitchOn = true;
                    initiateLocationRequest();
                }
            }
        });
    }

    private void checkPhoneState() {

    }

    private void onSwitchDisabled() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        LayoutInflater li = getLayoutInflater();
        View promptsView = li.inflate(R.layout.dialog_gps_permission_layout, null);
        TextView title = (TextView) promptsView.findViewById(R.id.dialog_title);
        TextView body = (TextView) promptsView.findViewById(R.id.dialog_body);
        title.setText("Turn off gps");
        body.setText("Do you want to turn off location fetching ?");
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gpsSwitch.setChecked(false);
                        editor.putString("gpsSwitch", "OFF");
                        editor.apply();
                        isSwitchOn = false;
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gpsSwitch.setChecked(true);
                        editor.putString("gpsSwitch", "ON");
                        editor.apply();
                        isSwitchOn = true;
                    }
                });
        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void onSwitchEnabled() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("gpsSwitch", "ON");
        editor.apply();
    }

    private void initiateLocationRequest() {
        Toast.makeText(MainActivity.this, "Turning ON GPS switch", Toast.LENGTH_LONG);
        new LocationFetchHelper(this, new FetchLocationSuccessListener() {
            @Override
            public void onLocationFetched(Location location) {
                if (!LocationFetchHelper.isMyServiceRunning(MainActivity.this.getApplicationContext(), ForegroundLocationService.class)) {
                    GPSSwitchRelated.onGpsSwitchEnable(MainActivity.this, location, true);
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            initiateLocationService();
                        }
                    });
                }
            }
        }, new FetchLocationFailureListener() {
            @Override
            public void onLocationFetchFailed(String errorMessage) {
                onSwitchDisabled();
            }
        }, false, false);
    }

    private void initiateLocationService() {
        new LocationFetchHelper(MainActivity.this, null, new FetchLocationFailureListener() {
            @Override
            public void onLocationFetchFailed(String errorMessage) {
                GPSSwitchRelated.onGpsSwitchDisable(MainActivity.this, false);
            }
        }, true, false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        },2000);
    }
}
