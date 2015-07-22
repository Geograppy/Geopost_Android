package com.geograppy.geopost.classes;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;

import com.geograppy.geopost.GeopostMapFragment;
import com.geograppy.geopost.MainActivity;
import com.geograppy.geopost.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONException;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by benito on 02/06/15.
 */
public class NotificationControl implements OnNotifcationsReceived, OnNotificationClickOrRemove {

    private MainActivity a;
    private double lon;
    private double lat;
    private NotificationCompat.Builder mBuilder;
    private int mNotificationId = 001;
    private NotificationManager mNotifyMgr;
    public boolean sendNotifications = false;
    private ArrayList<GeopostNotification> notifications;
    private static volatile NotificationControl instance;


    private NotificationControl(MainActivity a){
        this.a = a;
        this.mNotifyMgr = (NotificationManager) a.getSystemService(a.NOTIFICATION_SERVICE);
    }

    public static synchronized NotificationControl getInstance(MainActivity a){
        if (instance == null) instance = new NotificationControl(a);
        return instance;
    }

    public void start(LatLng latLng){
        if (latLng.latitude != 0 || latLng.longitude != 0){
            lon = latLng.longitude;
            lat = latLng.latitude;
            getNotifications();
        }
    }
    public void setLat(double latitude){
        lat = latitude;
    }

    public void setLon(double longitude){
        lon = longitude;
    }

    public void start(){
        getNotifications();
    }

    private void getNotifications(){
        String entity = null;
        try {
            entity = getNotificationsJson();
            new GetNotificationsAsync(a, this).execute(entity);
        } catch (JSONException e) {
            e.printStackTrace();

        }
    }

    private String getNotificationsJson()throws JSONException {

        // Add your data
        JSONStringer post = new JSONStringer()
                .object()
                .key("getNotificationsByUseridRequest")
                .object()
                .key("Buffer").value(Helpers.getBufferDistance(a))
                .key("Lon").value(lon)
                .key("Lat").value(lat)
                .key("Userid").value(Helpers.getUseridFromPreferences(a))
                .endObject()
                .endObject();


        return post.toString();
    }

    private void createOrUpdateNotification(){
        IconGenerator tc = new IconGenerator(a);
        tc.setBackground(a.getResources().getDrawable(R.mipmap.ic_launcher));
        Bitmap icon = tc.makeIcon();
        mBuilder = new NotificationCompat.Builder(a)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(icon)
                .setContentTitle(a.getResources().getString(R.string.notification_title));
        setNotificationText();
        setNotificationAction();
        show();
    }

    private void setNotificationText(){
        int notificationsType = getNotificationType();

        if (notificationsType == 0) return;
        else if (notificationsType == 1){
            mBuilder.setContentText(a.getResources().getString(R.string.notification_conversations_found_text));
        }
        else if (notificationsType == 2){
            mBuilder.setContentText(a.getResources().getString(R.string.notification_comment_found_text));
        }
        else if (notificationsType == 3){
            mBuilder.setContentText(a.getResources().getString(R.string.notifications_comment_and_converations_found_text));
        }
    }

    private int getNotificationType(){
        int type = 0;
        for (int ii = 0; ii < notifications.size(); ii++){
            if (notifications.get(ii).NotificationType == 1){
                if(type == 3) continue;
                else if (type == 2) type = 3;
                else type = 1;
            }
            else{
                if (type == 3) continue;
                else if (type == 1) type = 3;
                else type = 2;
            }
        }
        return type;
    }

    private void setNotificationAction(){
        Intent resultIntent = new Intent(a, MainActivity.class);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        a,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setAutoCancel(true);
    }

    private void show(){
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    public void removeAll(){
        mNotifyMgr.cancelAll();
    }

    @Override
    public void onNotifcationsReceived(ArrayList<GeopostNotification> notificationsResponse) {
        if (notificationsResponse != null && notificationsResponse.size() > 0){
            updateNotificatons(notificationsResponse);
            a.updateNotificationsBadge(notifications.size());
            if (sendNotifications){
                createOrUpdateNotification();
            }
        }
        else mNotifyMgr.cancel(mNotificationId);
    }

    @Override
    public void onNotificationClickOrRemove(GeopostNotification notification) {
        notifications.remove(notification);
        a.updateNotificationsBadge(notifications.size());
    }

    @Override
    public void onClear() {
        notifications.clear();
        a.updateNotificationsBadge(notifications.size());
    }

    public void updateNotificatons(ArrayList<GeopostNotification> list){
        if (notifications == null) notifications = new ArrayList<GeopostNotification>();
        notifications.addAll(list);
        removeDuplicates();
    }

    public void removeDuplicates() {
        // ... the list is already populated
        Set<GeopostNotification> s = new TreeSet<>(new Comparator<GeopostNotification>() {

            @Override
            public int compare(GeopostNotification o1, GeopostNotification o2) {
                if (o1.ConversationId == o2.ConversationId)
                return 0;
                else return 1;
            }
        });
        s.addAll(notifications);
        notifications = new ArrayList<GeopostNotification>(s);
    }

    public void showNotificationsList(){
        if (notifications == null || notifications.size() == 0) return;
        final Dialog dialog = new ShowNotificationsListDialog(a,notifications, this);

        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().getAttributes().windowAnimations = R.style.add_geopost_dialog_animation;

        dialog.show();
    }


}
