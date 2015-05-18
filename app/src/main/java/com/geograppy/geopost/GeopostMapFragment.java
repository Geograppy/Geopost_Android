package com.geograppy.geopost;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.util.Log;
import android.widget.FrameLayout;

import com.geograppy.geopost.classes.ConversationGeom;
import com.geograppy.geopost.classes.CustomDialogClass;
import com.geograppy.geopost.classes.GetConversationsWithinBufferAsync;
import com.geograppy.geopost.classes.Helpers;
import com.geograppy.geopost.classes.OnTaskCompleted;
import com.geograppy.geopost.classes.ShowGeopostFromMarker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONException;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class GeopostMapFragment extends SupportMapFragment implements GoogleMap.OnMapLongClickListener, OnTaskCompleted, OnMapReadyCallback, ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationRequest mLocationRequest;
    private Marker mMarker;
    private Context mContext;
    private LayoutInflater mInflater;
    private FrameLayout        llLayout;
    private FragmentActivity    fragmentActivity;
    private double currentLatitude;
    private double currentLongitude;
    private Boolean initialLocationSet;
    private Map<Marker, ConversationGeom> markersData = new HashMap<Marker, ConversationGeom>();
    private SupportMapFragment mMapFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        super.onCreate(savedInstanceState);
        fragmentActivity = (FragmentActivity) super.getActivity();
        llLayout    = (FrameLayout)    inflater.inflate(R.layout.fragment_geopost_map, container, false);
        initialLocationSet = false;
        //llLayout.findViewById(R.id.activity_frame);
        //LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //View activityView = layoutInflater.inflate(R.layout.activity_map, null,false);// add the custom layout of this activity to frame layout.
        //frameLayout.addView(activityView);
        //setupRepository();


        return llLayout;

        //return this.findViewById(android.R.id.content).getRootView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentManager fm = getChildFragmentManager();
        mMapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map);
        if (mMapFragment == null) {
            mMapFragment = SupportMapFragment.newInstance();
            fm.beginTransaction().replace(R.id.map, mMapFragment).commit();
        }
        mMapFragment.getMapAsync(this);
    }
    private void showConversationsOnMap(){
        String entity = null;
        try {
            entity = getConversationsJson();
            new GetConversationsWithinBufferAsync(super.getActivity(), this).execute(entity);
        } catch (JSONException e) {
            e.printStackTrace();

        }
    }



    private String getConversationsJson()throws JSONException {

        // Add your data
        JSONStringer post = new JSONStringer()
                .object()
                .key("getConversationsWithinBufferRequest")
                .object()
                .key("Buffer").value(Helpers.getBufferDistance(super.getActivity()))
                .key("Lon").value(currentLongitude)
                .key("Lat").value(currentLatitude)
                .endObject()
                .endObject();


        return post.toString();
    }


    protected void setupTouchListeners(){
        mMap.setOnMapLongClickListener(this);

    }
    @Override
    public void onMapLongClick(LatLng latLng) {
        // custom dialog
        final Dialog dialog = new CustomDialogClass(super.getActivity(), this, latLng);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().getAttributes().windowAnimations = R.style.add_geopost_dialog_animation;

        dialog.show();
    }

    protected void buildGoogleApiClient() {
        GoogleApiClient.Builder builder;
        builder = new GoogleApiClient.Builder(fragmentActivity);
        builder.addConnectionCallbacks(this);
        builder.addOnConnectionFailedListener(this);
        builder.addApi(LocationServices.API);
        mGoogleApiClient = builder.build();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLatitude, currentLongitude), 20));
        setupTouchListeners();

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Location services connected.");

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


    }

    private void setUpdateLocationRequest(){
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(60 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(10 * 1000); // 1 second, in milliseconds
    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        if (!initialLocationSet) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
            showConversationsOnMap();
            initialLocationSet = true;
        }
    }

    protected void controlMarker(ConversationGeom conversation){
        IconGenerator tc = new IconGenerator(super.getActivity());
        Bitmap bmp = tc.makeIcon(conversation.Title);
        LatLng latlng = new LatLng(conversation.Lat, conversation.Lon);
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(bmp)).
                position(latlng);

        Marker marker = mMap.addMarker(markerOptions);
        markersData.put(marker, conversation);
    }

    private void controlMarker(ArrayList<ConversationGeom> conversations){

        setMarkerTapListener();
        if (conversations != null) {
            for (int i = 0; i < conversations.size(); i++) {

                if (markersData.containsValue(conversations.get(i))) continue;
                IconGenerator tc = new IconGenerator(super.getActivity());
                Bitmap bmp = tc.makeIcon(conversations.get(i).Title);
                LatLng latlng = new LatLng(conversations.get(i).Lat, conversations.get(i).Lon);
                MarkerOptions markerOptions = new MarkerOptions().
                        icon(BitmapDescriptorFactory.fromBitmap(bmp)).
                        position(latlng);

                Marker marker = mMap.addMarker(markerOptions);
                markersData.put(marker, conversations.get(i));

            }
        }

    }

    private void setMarkerTapListener(){
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {

            @Override
            public boolean onMarkerClick(Marker arg0) {
                ConversationGeom conversation = markersData.get(arg0);
                final Dialog dialog = new ShowGeopostFromMarker(fragmentActivity, conversation);

                dialog.getWindow().setGravity(Gravity.BOTTOM);
                dialog.getWindow().getAttributes().windowAnimations = R.style.add_geopost_dialog_animation;

                dialog.show();

                return true;
            }

        });
    }

    private void setupMap(){
        //if (mMapFragment == null) mMapFragment = (SupportMapFragment)fragmentActivity.getSupportFragmentManager()
        //        .findFragmentById(R.id.map);
        //mMapFragment.getMapAsync(this);
        //getMapAsync(this);
        if (mMapFragment == null) {
            FragmentManager fm = getChildFragmentManager();
            mMapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map);
            if (mMapFragment == null) {
                mMapFragment = SupportMapFragment.newInstance();
                fm.beginTransaction().replace(R.id.map, mMapFragment).commit();
                if (mMap == null) mMapFragment.getMapAsync(this);
            }
        }

    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the MapFragment.
            setupMap();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        setUpdateLocationRequest();
        buildGoogleApiClient();
        if (mGoogleApiClient != null) {mGoogleApiClient.connect();}
        showConversationsOnMap();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(fragmentActivity, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }


    @Override
    public void onTaskCompleted(ArrayList<ConversationGeom>conversations) {
        controlMarker(conversations);
    }




}

