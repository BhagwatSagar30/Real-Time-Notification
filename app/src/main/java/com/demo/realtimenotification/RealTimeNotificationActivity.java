package com.demo.realtimenotification;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class RealTimeNotificationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /**
     * This is used for checking the back press time count.
     */
    private long back_pressed;

    /**
     * for displaying the Log error
     */
    public static final String TAG = "Activity";

    /**
     * used for Starting the background service for sending notification
     */
    private PendingIntent mGeofencePendingIntent;

    /**
     * Location access permission code
     */
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 101;

    /**
     * instance of making request Geofencing
     */
    private GeofencingRequest mGeofencingRequest;

    /**
     * Instance of api for accessing the google services
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * for storing the destination latlong
     */
    private double destinationLatitude, destinationLongitude;

    /**
     * getting the current location from user
     */
    private PlaceAutocompleteFragment currentlocationAutoCompleteFragment;

    /**
     * getting the destination location from user
     */
    private PlaceAutocompleteFragment destinationAutoCompleteFragment;

    /**
     * Staring the service and api request
     */
    private Button startButton;

    /**
     * Stop the Google mGeofencingRequest
     */
    private Button stopButton;

    /**
     * for getting the location services
     */
    private GeofencingClient mGeofencingClient;

    /**
     * checking that current location enter or not
     */
    private boolean isCurrentValueSelected;

    /**
     * Broadcast receiver get push notification.
     */
    private BroadcastReceiver mNotificationBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_real_time_notification);

        initialiseUIComponent();

        handleButtonAndAutoPlaceClickListeners();

        mNotificationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction().equals(Constants.PUSH_NOTIFICATION)) {

                    startButton.setVisibility(View.GONE);

                    stopButton.setVisibility(View.VISIBLE);

                }

            }
        };

    }

    /**
     * Unregister the broadcast receiver before moving to any other activity or application
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mNotificationBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(mNotificationBroadcastReceiver,
                new IntentFilter(Constants.PUSH_NOTIFICATION));
    }

    /**
     * This method handle the button click event.
     * perform the particular operation on click
     */
    private void handleButtonAndAutoPlaceClickListeners() {

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checkPlayServices()) {

                    //Checking runtime permission
                    if (ActivityCompat.checkSelfPermission(RealTimeNotificationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(RealTimeNotificationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(RealTimeNotificationActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
                    }

                    initialiseGoogleApi();
                }

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopGeoFencing();

            }
        });

        currentlocationAutoCompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

            }

            @Override
            public void onError(Status status) {

                Log.i(TAG, "An error occurred: " + status);
            }

        });

        destinationAutoCompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                place.getLatLng();

                Log.i(TAG, "Place: " + place.getName());

                LatLng latLng = place.getLatLng();

                destinationLatitude = latLng.latitude;

                destinationLongitude = latLng.longitude;

                startButton.setVisibility(View.VISIBLE);

            }

            @Override
            public void onError(Status status) {

                Log.i(TAG, "An error occurred: " + status);
            }

        });


    }

    /**
     * This Method check id Google play serviced are avialable or not on devices.
     *
     * @return return result according to services available or not
     */
    private boolean checkPlayServices() {


        int response = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

        if (response != ConnectionResult.SUCCESS) {

            GoogleApiAvailability.getInstance().getErrorDialog(this, response, 1).show();

            Log.d(TAG, "On Your Devices Google Play Service  is Not Available");

            return false;

        } else {

            //checking gps is enable or not
            final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (!(manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER))) {

                displayMessageToEnableGPS();
            }

            isNetworkConnected();

            Log.d(TAG, "Google play service available");

            return true;

        }

    }

    private void displayMessageToEnableGPS() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(getString(R.string.gps_enable_message))

                .setCancelable(false)

                .setPositiveButton(getString(R.string.enable_text), new DialogInterface.OnClickListener() {

                    public void onClick(final DialogInterface dialog, final int id) {

                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                    }

                })

                .setNegativeButton(getString(R.string.cancel_text), new DialogInterface.OnClickListener() {

                    public void onClick(final DialogInterface dialog, final int id) {

                        dialog.cancel();

                    }

                });

        final AlertDialog alert = builder.create();

        alert.show();
    }

    /**
     * This method check if network is connected or not
     */
    private void isNetworkConnected() {

        ConnectivityManager mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo mNetworkInfo = null;

        if (mConnectivityManager != null) {

            mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();

        }

        if (mNetworkInfo == null || !mNetworkInfo.isConnectedOrConnecting()) {

            AlertDialog dialog = new AlertDialog.Builder(RealTimeNotificationActivity.this)
                    .setTitle(getString(R.string.connection_error))
                    .setMessage(getString(R.string.check_internet))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }

                    })
                    .create();

            dialog.show();


        }

    }


    /**
     * Create GoogleApiClient instance
     */
    public void initialiseGoogleApi() {


        mGoogleApiClient = new GoogleApiClient.Builder(this)

                .addApi(LocationServices.API)

                .addConnectionCallbacks(this)

                .addOnConnectionFailedListener(this)

                .build();

        mGoogleApiClient.connect();

        mGeofencingClient = LocationServices.getGeofencingClient(this);

    }

    /**
     * This method return pending instance if intent is initialise then return initialise  instance
     * otherwise  create new intent instance
     *
     * @return instance of pending intent
     */
    private PendingIntent getGeofencePendingIntent() {

        if (mGeofencePendingIntent != null) {

            return mGeofencePendingIntent;

        }

        Intent intent = new Intent(this, SendNotificationIntentService.class);

        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * This Method initialise all UI component or variables.
     */
    private void initialiseUIComponent() {

        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        updateToolbarText(getString(R.string.dashboard_text));

        currentlocationAutoCompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.source_autocomplete_fragment);

        destinationAutoCompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.destination_autocomplete_fragment);

        startButton = findViewById(R.id.start_button);

        stopButton = findViewById(R.id.stop_button);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_real_time_notification, menu);

        return true;
    }

    /**
     * This method will update the tool bar title
     *
     * @param title name which is display as title to tool bar
     */
    public void updateToolbarText(String title) {

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {

            actionBar.setTitle(title);

        }

    }

    /**
     * this method set back arrow button enable or disable.
     *
     * @param showBackButtonFlag boolean value for changing back button according to requirement.
     */
    public void changeToolbar(boolean showBackButtonFlag) {

        try {

            if (getSupportActionBar() != null) {

                getSupportActionBar().setDisplayHomeAsUpEnabled(showBackButtonFlag);

                getSupportActionBar().setDisplayShowHomeEnabled(showBackButtonFlag);

            }

        } catch (Exception ex) {

            ex.printStackTrace();

        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_about_me) {

            Fragment fragment = new AboutMeFragment();

            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

            fragmentTransaction.add(R.id.fragment_container, fragment);

            fragmentTransaction.addToBackStack(null);

            fragmentTransaction.commit();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        if (getSupportFragmentManager().getBackStackEntryCount() < 1) {

            if (back_pressed + 2000 > System.currentTimeMillis()) {

                finish();

            } else {

                Toast.makeText(getBaseContext(), "Press once again to exit", Toast.LENGTH_SHORT).show();

                back_pressed = System.currentTimeMillis();

            }

        } else {

            super.onBackPressed();

            changeToolbar(false);

            updateToolbarText(getResources().getString(R.string.dashboard_text));

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        startGeofencing();

        startLocationMonitor();

    }

    @Override
    public void onConnectionSuspended(int i) {

        Log.d(TAG, "Google Connection Suspended");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Log.e(TAG, "Connection Failed:" + connectionResult.getErrorMessage());

    }

    /**
     *
     */
    private void startGeofencing() {

        Log.d(TAG, "Start geofencing monitoring call");

        mGeofencePendingIntent = getGeofencePendingIntent();

        mGeofencingRequest = new GeofencingRequest.Builder()

                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)

                .addGeofence(getGeofence())

                .build();


        if (!mGoogleApiClient.isConnected()) {

            Log.d(TAG, "Google API client not connected");

        } else {

            // checking runtime location permission.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            mGeofencingClient.addGeofences(mGeofencingRequest, mGeofencePendingIntent)
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            Log.d(TAG, "Successfully Geofencing Connected");

                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            Log.d(TAG, "Failed to add geofences");

                        }
                    });

        }

    }

    private void startLocationMonitor() {

        LocationRequest locationRequest = LocationRequest.create()

                .setInterval(2000)

                .setFastestInterval(1000)

                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    Log.d(TAG, "Location Change!!!  Current Location (Lat,long) is  " + location.getLatitude() + " " + location.getLongitude());

                }

            });

        } catch (SecurityException e) {

            Log.d(TAG, e.getMessage());
        }
    }

    private Geofence getGeofence() {

        return new Geofence.Builder()

                .setRequestId(Constants.GEOFENCE_ID)

                .setExpirationDuration(Geofence.NEVER_EXPIRE)

                .setCircularRegion(destinationLatitude, destinationLongitude, Constants.GEOFENCE_RADIUS_IN_METERS)

                .setNotificationResponsiveness(1000)

                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)

                .build();
    }

    /**
     * This method stop the the Geofencing or removed once it reached to destination
     */
    private void stopGeoFencing() {

        mGeofencePendingIntent = getGeofencePendingIntent();

        mGeofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        Log.d(TAG, "Geofences removed");

                    }

                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        // Failed to remove geofences
                        Log.d(TAG, "Failed to remove geofences");

                    }
                });

    }

}
