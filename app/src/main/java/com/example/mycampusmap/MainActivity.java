package com.example.mycampusmap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.api.ResolvableApiException;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final int REQUEST_CHECK_SETTINGS = 2000;
    private WebView webView;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Handler locationSettingsHandler;
    private Runnable locationSettingsRunnable;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up WebView to load the map (OpenStreetMap)
        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "Android"); // Add JavaScript Interface
        webView.loadUrl("file:///android_asset/map.html"); // Load your map.html file

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Request location permissions if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            checkLocationSettings();
        }

        // Set up a handler to check location settings periodically
        locationSettingsHandler = new Handler();
        locationSettingsRunnable = new Runnable() {
            @Override
            public void run() {
                checkLocationSettings();  // Re-check location settings periodically
                locationSettingsHandler.postDelayed(this, 10000);  // Check every 10 seconds
            }
        };
        locationSettingsHandler.post(locationSettingsRunnable);

        // Monitor internet connectivity
        setupInternetMonitoring();
    }

    // JavaScript Interface to interact with SharedPreferences
    public class WebAppInterface {
        Context context;

        WebAppInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void saveCredentials(String username, String password) {
            SharedPreferences sharedPref = context.getSharedPreferences("CampusMapPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("username", username);
            editor.putString("password", password);
            editor.apply();
            runOnUiThread(() -> Toast.makeText(context, "Credentials saved!", Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public String getUsername() {
            SharedPreferences sharedPref = context.getSharedPreferences("CampusMapPrefs", Context.MODE_PRIVATE);
            return sharedPref.getString("username", "");
        }

        @JavascriptInterface
        public String getPassword() {
            SharedPreferences sharedPref = context.getSharedPreferences("CampusMapPrefs", Context.MODE_PRIVATE);
            return sharedPref.getString("password", "");
        }
    }

    private void setupInternetMonitoring() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Define a network request for internet-capable networks
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Internet is connected", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                runOnUiThread(() -> showInternetDialog());
            }
        };

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    private void showInternetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Internet Connection Lost")
                .setMessage("It seems like your internet connection is off. Would you like to turn it on?")
                .setPositiveButton("Go to Settings", (dialog, which) -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> getLocationUpdates());

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (Exception ignored) {
                }
            } else {
                Toast.makeText(MainActivity.this, "Location services are unavailable", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    updateMapLocation(latitude, longitude);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void updateMapLocation(double latitude, double longitude) {
        String jsCode = "updateLocation(" + latitude + ", " + longitude + ")";
        webView.evaluateJavascript(jsCode, null);  // Pass live location and routing updates
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }

        if (locationSettingsHandler != null) {
            locationSettingsHandler.removeCallbacks(locationSettingsRunnable);
        }

        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationSettings();
        } else {
            Toast.makeText(this, "Permission denied to access location", Toast.LENGTH_SHORT).show();
        }
    }
}



