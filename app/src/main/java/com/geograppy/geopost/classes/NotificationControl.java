package com.geograppy.geopost.classes;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import com.geograppy.geopost.MainActivity;
import com.geograppy.geopost.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONException;
import org.json.JSONStringer;

import java.util.ArrayList;

/**
 * Created by benito on 02/06/15.
 */
public class NotificationControl implements OnTaskCompleted {

    private Activity a;
    private double lon;
    private double lat;
    private NotificationCompat.Builder mBuilder;
    private int mNotificationId = 001;
    private NotificationManager mNotifyMgr;

    public NotificationControl(Activity a){
        this.a = a;
        this.mNotifyMgr = (NotificationManager) a.getSystemService(a.NOTIFICATION_SERVICE);
    }

    public void start(LatLng latLng){
        if (latLng.latitude != 0 || latLng.longitude != 0){
            lon = latLng.longitude;
            lat = latLng.latitude;
            getConversationsByBuffer();
        }
    }

    private void getConversationsByBuffer(){
        String entity = null;
        try {
            entity = getConversationsJson();
            new GetConversationsWithinBufferAsync(a, this).execute(entity);
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
                .key("Buffer").value(Helpers.getBufferDistance(a))
                .key("Lon").value(lon)
                .key("Lat").value(lat)
                .endObject()
                .endObject();


        return post.toString();
    }

    @Override
    public void onTaskCompleted(ArrayList<ConversationGeom> conversations) {

        if (conversations.size() > 0){
            if (Helpers.containsNotNotifiedConversationIds(conversations, a)){
                createOrUpdateNotification();
                Helpers.setNotifiedConversationsIds(conversations, a);
            }
        }
        else mNotifyMgr.cancel(mNotificationId);
    }

    private void createOrUpdateNotification(){
        IconGenerator tc = new IconGenerator(a);
        tc.setBackground(a.getResources().getDrawable(R.mipmap.ic_launcher));
        Bitmap icon = tc.makeIcon();
        mBuilder = new NotificationCompat.Builder(a)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setLargeIcon(icon)
                        .setContentTitle(a.getResources().getString(R.string.notification_conversations_found_title))
                        .setContentText(a.getResources().getString(R.string.notification_conversations_found_text));
        setNotificationAction();
        show();
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
}
