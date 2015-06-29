package com.geograppy.geopost;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.geograppy.geopost.classes.ConversationGeom;
import com.geograppy.geopost.classes.CustomDialogClass;
import com.geograppy.geopost.classes.GetConversationsWithinBufferAsync;
import com.geograppy.geopost.classes.Helpers;
import com.geograppy.geopost.classes.NotificationControl;
import com.geograppy.geopost.classes.OnTaskCompleted;
import com.geograppy.geopost.classes.ShowGeopostFromMarker;
import com.geograppy.geopost.classes.TouchableWrapper;
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


public class GeopostMapFragment extends SupportMapFragment implements GoogleMap.OnMapLongClickListener, OnTaskCompleted, OnMapReadyCallback, ConnectionCallbacks, OnConnectionFailedListener, LocationListener, TouchableWrapper.UpdateMapAfterUserInterection {

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
    private double currentGpsLatitude;
    private double currentGpsLongitude;
    private double currentMapCenterLat;
    private double currentMapCenterLon;
    private Boolean initialLocationSet;
    private Map<Marker, ConversationGeom> markersData = new HashMap<Marker, ConversationGeom>();
    private SupportMapFragment mMapFragment;
    public View mOriginalContentView;
    public TouchableWrapper mTouchView;
    public NotificationControl notificationManager;
    public boolean sendNotifications = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mOriginalContentView = super.onCreateView(inflater, container, savedInstanceState);
        super.onCreate(savedInstanceState);
        fragmentActivity = (FragmentActivity) super.getActivity();
        llLayout    = (FrameLayout)    inflater.inflate(R.layout.fragment_geopost_map, container, false);
        initialLocationSet = false;
        sendNotifications = false;
        notificationManager = new NotificationControl(super.getActivity());
        //llLayout.findViewById(R.id.activity_frame);
        //LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //View activityView = layoutInflater.inflate(R.layout.activity_map, null,false);// add the custom layout of this activity to frame layout.
        //frameLayout.addView(activityView);
        //setupRepository();
        //mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);
        mTouchView = new TouchableWrapper(super.getActivity(), this);
        mTouchView.addView(mOriginalContentView);
        mTouchView.addView(llLayout);
        return mTouchView;

        //return llLayout;

        //return this.findViewById(android.R.id.content).getRootView();
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        sendNotifications = false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentManager fm = getChildFragmentManager();
        mMapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map);
        if (mMapFragment == null) {
            mMapFragment = SupportMapFragment.newInstance();
            fm.beginTransaction().replace(R.id.drawer_layout, mMapFragment).commit();
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
                .key("Lon").value(currentMapCenterLon)
                .key("Lat").value(currentMapCenterLat)
                .endObject()
                .endObject();


        return post.toString();
    }


    protected void setupMapListeners(){
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                onUpdateMapAfterUserInterection();
            }
        });

    }
    @Override
    public void onMapLongClick(LatLng latLng) {
        // custom dialog
        final Dialog dialog = new CustomDialogClass(super.getActivity(), this, latLng);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().getAttributes().windowAnimations = R.style.add_geopost_dialog_animation;

        dialog.show();
    }

    public void postToMyLocation(){
        goToMyLocation();
        if (currentLocationFound()){
            LatLng latLng = new LatLng(currentGpsLatitude, currentGpsLongitude);
            final Dialog dialog = new CustomDialogClass(super.getActivity(), this, latLng);
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.getWindow().getAttributes().windowAnimations = R.style.add_geopost_dialog_animation;

            dialog.show();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

   private boolean currentLocationFound(){
        return currentGpsLatitude != 0 || currentGpsLongitude != 0;
    }

    protected void buildGoogleApiClient() {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()){
            GoogleApiClient.Builder builder;
            builder = new GoogleApiClient.Builder(fragmentActivity);
            builder.addConnectionCallbacks(this);
            builder.addOnConnectionFailedListener(this);
            builder.addApi(LocationServices.API);
            mGoogleApiClient = builder.build();
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentGpsLatitude, currentGpsLongitude), 20));
        setupMapListeners();

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Location services connected.");

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


    }

    private void setUpdateLocationRequest(){
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 60 seconds, in milliseconds
                .setFastestInterval(10 * 1000); // 1 second, in milliseconds
    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        currentGpsLatitude = location.getLatitude();
        currentGpsLongitude = location.getLongitude();

        LatLng latLng = new LatLng(currentGpsLatitude, currentGpsLongitude);
        if (sendNotifications) notificationManager.start(latLng);
        else notificationManager.removeAll();

        if (!initialLocationSet) {
            if (currentGpsLatitude != 0 && currentGpsLongitude !=0) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                currentMapCenterLat = currentGpsLatitude;
                currentMapCenterLon = currentGpsLongitude;
            }
            else {mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 3));}
            //showConversationsOnMap();
            initialLocationSet = true;
        }
    }

    public void zoomToFullExtent(){
        if (mMap != null) mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 1));
    }

    public void goToMyLocation(){
        if (currentGpsLongitude != 0 || currentGpsLatitude != 0){
        LatLng latLng = new LatLng(currentGpsLatitude, currentGpsLongitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));}
        else {
            Toast.makeText(super.getActivity(), R.string.noGPS, Toast.LENGTH_SHORT).show();
            zoomToFullExtent();
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
                int iconPosition = 40;
                IconGenerator tc = new IconGenerator(super.getActivity());
                tc.setColor(getResources().getColor(R.color.actionbar));
                tc.setContentPadding(100, iconPosition, 20, iconPosition);
                tc.setTextAppearance(R.style.markerText);

                Bitmap bmp = tc.makeIcon(conversations.get(i).Title);

                Canvas canvas1 = new Canvas(bmp);


                Paint color = new Paint();
                color.setColor(Color.BLACK);


                canvas1.drawBitmap(BitmapFactory.decodeResource(getResources(),
                        R.drawable.notification_icon), 5, iconPosition, color);






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
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

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
        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected()) {mGoogleApiClient.connect();}
        sendNotifications = false;
        notificationManager.removeAll();
        //showConversationsOnMap();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            sendNotifications = true;
            //LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            //mGoogleApiClient.disconnect();
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

    @Override
    public void onUpdateMapAfterUserInterection() {
        LatLng center = mMap.getCameraPosition().target;
        currentMapCenterLat = center.latitude;
        currentMapCenterLon = center.longitude;
        if (currentMapCenterLon != 0 || currentMapCenterLat != 0)showConversationsOnMap();
        else {zoomToFullExtent();}
    }
}

