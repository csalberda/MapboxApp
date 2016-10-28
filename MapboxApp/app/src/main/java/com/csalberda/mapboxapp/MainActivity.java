package com.csalberda.mapboxapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    //VARS
    private Context context;

    private Toolbar toolbar;
    private MapView mapView;
    private Icon markerIcon;

    private MapboxMap map;
    private FloatingActionButton floatingActionButton;
    private LocationServices locationServices;

    private int iCurStyle = 0;
    private String[] styleURLs = {
            "mapbox://styles/mapbox/light-v9",
            "mapbox://styles/mapbox/streets-v9",
            "mapbox://styles/mapbox/outdoors-v9",
            "mapbox://styles/mapbox/dark-v9",
            "mapbox://styles/mapbox/satellite-v9",
            "mapbox://styles/mapbox/satellite-streets-v9"};
    private static final int PERMISSIONS_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        MapboxAccountManager.start(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);

        //INIT THE MENU
        initToolBar();

        //GET LOCATION SERVICES
        locationServices = LocationServices.getLocationServices(MainActivity.this);

        //CREATES AN ICON FOR THE CUSTOM MARKER
        IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
        Drawable iconDrawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.pin_custom_fishing);
        markerIcon = iconFactory.fromDrawable(iconDrawable);

        //CREATE OUR MAP VIEW
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {

                map = mapboxMap;

                // Interact with the map using mapboxMap here.
                // Customize map with markers, polylines, etc.

                JSONObject obj;
                try {
                    obj = new JSONObject(loadJSONFromAsset());
                    JSONArray jArr = obj.getJSONArray("pointsArray");

                    //FOR EACH POINT, PLACE A MARKER WITH RESPECTIVE DATA
                    for (int i=0; i<jArr.length(); i++){
                        String pointName = jArr.getJSONObject(i).getString("pointName");
                        double latitude = jArr.getJSONObject(i).getDouble("latitude");
                        double longitude = jArr.getJSONObject(i).getDouble("longitude");

                        // Add a marker to the map
                        mapboxMap.addMarker(new MarkerViewOptions()
                                .position(new LatLng(latitude, longitude))
                                .title(pointName)
                                .icon(markerIcon));
                    }
                } catch (JSONException e) { e.printStackTrace(); }

            }
        });

        //CENTER ON USER BUTTON
        floatingActionButton = (FloatingActionButton) findViewById(R.id.location_toggle_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map != null)
                    startGps();
            }
        });

        //NEW MARKER BUTTON
        floatingActionButton = (FloatingActionButton) findViewById(R.id.new_marker_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                double lat = map.getCameraPosition().target.getLatitude();
                double lng = map.getCameraPosition().target.getLongitude();

                // Add a marker to the map
                map.addMarker(new MarkerViewOptions()
                        .position(new LatLng(lat, lng))
                        .title("New Point")
                        .icon(markerIcon));
            }
        });
    }

    /************************************************************************
     * Initializes the toolbar with the menu buttons
     ***********************************************************************/
    public void initToolBar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Explore");
        setSupportActionBar(toolbar);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_search:
                Toast.makeText(getApplicationContext(), "Search pressed", Toast.LENGTH_SHORT).show();
                break;
            case R.id.icon_layers:
                String newStyle = styleURLs[(iCurStyle+1)%styleURLs.length];
                map.setStyleUrl(newStyle);
                Toast.makeText(getApplicationContext(),
                        "Loading " + newStyle.substring(newStyle.lastIndexOf("/")+1, newStyle.length()-1),
                        Toast.LENGTH_SHORT).show();
                iCurStyle++;
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /************************************************************************
     * Starts GPS/Location service
     ***********************************************************************/
    private void startGps() {
        // Check if user has granted location permission
        if (!locationServices.areLocationPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
        }
        else
            enableLocation();
    }

    /************************************************************************
     * Move camera to users location
     ***********************************************************************/
    private void enableLocation() {

            // If we have the last location of the user, we can move the camera to that position.
            Location lastLocation = locationServices.getLastLocation();
            if (lastLocation != null) {
                map.easeCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 14), 1000);
            }

            locationServices.addLocationListener(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.

                        map.easeCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 14), 1000);
                        locationServices.removeLocationListener(this);
                    }
                }
            });

        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation();
                Toast.makeText(getApplicationContext(), "Location Enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*************************************************************************************
     * Loads the points from a json file and returns a json object containing the data
     ************************************************************************************/
    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("points.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
