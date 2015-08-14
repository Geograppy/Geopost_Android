package com.geograppy.geopost;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


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
    private Map<UUID, Marker> markersData = new HashMap<UUID, Marker>();
    private Map<Marker, ConversationGeom> conversationsData = new HashMap<Marker, ConversationGeom>();
    private SupportMapFragment mMapFragment;
    public View mOriginalContentView;
    public TouchableWrapper mTouchView;
    public NotificationControl notificationManager;
    private long notificationPollingInterval;
    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();
    private double lastUpdateConversationsLat;
    private double lastUpdateConversationsLon;
    private static boolean showInstructionMarker = true;
    private UUID instructionMarkerUUID;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mOriginalContentView = super.onCreateView(inflater, container, savedInstanceState);
        super.onCreate(savedInstanceState);
        fragmentActivity = (FragmentActivity) super.getActivity();
        llLayout    = (FrameLayout)    inflater.inflate(R.layout.fragment_geopost_map, container, false);
        initialLocationSet = false;

        notificationManager = NotificationControl.getInstance((MainActivity) super.getActivity());
        notificationManager.sendNotifications = false;
        notificationPollingInterval = fragmentActivity.getResources().getInteger(R.integer.activePolling);
        setUpdateLocationRequest(R.integer.locationRequestInterval_active);
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
        notificationManager.sendNotifications = false;
        //worker.shutdown();
        notificationPollingInterval = fragmentActivity.getResources().getInteger(R.integer.notActivePolling);
        //getNotifications();
        setUpdateLocationRequest(R.integer.locationRequestInterval_inactive);
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

    private void setUpdateLocationRequest(int intervalSeconds){
        if (mLocationRequest != null){
            mLocationRequest.setInterval(intervalSeconds * 1000)        // 60 seconds, in milliseconds
                    .setFastestInterval(10 * 1000); // 1 second, in milliseconds
        }
    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        currentGpsLatitude = location.getLatitude();
        currentGpsLongitude = location.getLongitude();

        LatLng latLng = new LatLng(currentGpsLatitude, currentGpsLongitude);
        if (notificationManager == null) notificationManager = NotificationControl.getInstance((MainActivity) super.getActivity());
        notificationManager.setLat(currentGpsLatitude);
        notificationManager.setLon(currentGpsLongitude);
        getNotifications();

        if (!initialLocationSet) {
            if (currentGpsLatitude != 0 && currentGpsLongitude !=0) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                currentMapCenterLat = currentGpsLatitude;
                currentMapCenterLon = currentGpsLongitude;

            }
            else {mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 3));}
            showConversationsOnMap();
            initialLocationSet = true;
        }
    }

    public double getCurrentGpsLatitude(){
        return currentGpsLatitude;
    }

    public double getCurrentGpsLongitude(){
        return currentGpsLongitude;
    }

    private void getNotifications(){
        if (notificationManager == null) notificationManager = NotificationControl.getInstance((MainActivity) super.getActivity());
        if (notificationManager != null) notificationManager.start();
        Runnable task = new Runnable() {
            public void run() {
                while(true){
                    if (notificationManager== null) notificationManager = NotificationControl.getInstance((MainActivity) mMapFragment.getActivity());
                    if (notificationManager != null) notificationManager.start();
                    SystemClock.sleep(notificationPollingInterval);
                }
            }
        };
        worker.scheduleWithFixedDelay(task, 0, notificationPollingInterval, TimeUnit.MILLISECONDS);
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

    public void zoomToLatLng(LatLng latLng){
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
    }

    public void controlMarker(ArrayList<ConversationGeom> conversations){

        setMarkerTapListener();
        if (conversations != null) {
            for (int i = 0; i < conversations.size(); i++) {

                if (markersData.containsKey(conversations.get(i).ConvGuid)) continue;
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
                markersData.put(conversations.get(i).ConvGuid, marker);
                conversationsData.put(marker, conversations.get(i));
            }
        }

    }

    private void setMarkerTapListener(){
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker arg0) {
                ConversationGeom conversation = conversationsData.get(arg0);
                if (conversation.ConvGuid == instructionMarkerUUID) {
                    removeInstructionMarker();
                    return true;
                }
                zoomToLatLng(new LatLng(conversation.Lat, conversation.Lon));
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
        notificationManager.sendNotifications = false;
        //worker.shutdown();
        notificationPollingInterval = fragmentActivity.getResources().getInteger(R.integer.activePolling);
        //getNotifications();
        notificationManager.removeAll();
        setUpdateLocationRequest(R.integer.locationRequestInterval_active);
        //showConversationsOnMap();
    }

    public void showNotificationList(){

        if (currentGpsLatitude != 0 || currentGpsLongitude != 0){
            notificationManager.start(new LatLng(currentGpsLatitude, currentGpsLongitude));
        }
        notificationManager.showNotificationsList();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            notificationManager.sendNotifications = true;
            notificationPollingInterval = fragmentActivity.getResources().getInteger(R.integer.notActivePolling);
            setUpdateLocationRequest(R.integer.locationRequestInterval_inactive);
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
        if (conversations == null || conversations.size() ==0){
            lastUpdateConversationsLat = 0;
            lastUpdateConversationsLon = 0;
            if (showInstructionMarker){
                showInstructionMarker = false;
                ConversationGeom conversation = new ConversationGeom();
                conversation.Lat = currentMapCenterLat;
                conversation.Lon = currentMapCenterLon;
                conversation.Title = this.getResources().getString(R.string.instruction_marker_title);
                conversation.ConvGuid = UUID.randomUUID();
                instructionMarkerUUID = conversation.ConvGuid;
                conversations.add(conversation);
            }
        }
        controlMarker(conversations);
    }

    @Override
    public void onUpdateMapAfterUserInterection() {
        if (!initialLocationSet) initialLocationSet = true;
        LatLng center = mMap.getCameraPosition().target;
        currentMapCenterLat = center.latitude;
        currentMapCenterLon = center.longitude;
        if (currentMapCenterLat != 0 || currentMapCenterLon != 0){
            lastUpdateConversationsLat = currentMapCenterLat;
            lastUpdateConversationsLat = currentMapCenterLat;
            if (!showInstructionMarker) removeInstructionMarker();
            showConversationsOnMap();

        } else if (currentMapCenterLon == 0 && currentMapCenterLat == 0){
            zoomToFullExtent();
        }

    }

    private void removeInstructionMarker(){
        Marker marker = markersData.get(instructionMarkerUUID);
        marker.remove();
    }

    private boolean newMapLocationOutsideBuffer(){
        if (lastUpdateConversationsLat == 0 && lastUpdateConversationsLon == 0) return true;


        final int radius = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(currentMapCenterLat - lastUpdateConversationsLat);
        Double lonDistance = Math.toRadians(currentMapCenterLon - lastUpdateConversationsLon);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lastUpdateConversationsLat)) * Math.cos(Math.toRadians(currentMapCenterLat))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = radius * c * 1000; // convert to meters
        int buffer = super.getResources().getInteger(R.integer.buffer_for_conversations);
        return distance > buffer;
    }
}

