package com.geograppy.geopost.classes;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.geograppy.geopost.GeopostMapFragment;
import com.geograppy.geopost.MainActivity;
import com.geograppy.geopost.R;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by benito on 29/04/15.
 */
public class ShowNotificationsListDialog extends Dialog implements AdapterView.OnItemClickListener {
    public MainActivity c;
    public Dialog d;
    private ShowNotificationListAdapter<GeopostNotification> adapter;
    private ArrayList<GeopostNotification> notifications;

    public ShowNotificationsListDialog(Activity a, ArrayList<GeopostNotification> notifications) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = (MainActivity) a;
        this.notifications = notifications;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.show_notifications_list_dialog);
        loadNotificationsList();
    }

    public void loadNotificationsList() {
        if (notifications == null || notifications.size() == 0) {
            this.dismiss();
            return;
        }
        ListView listView = (ListView) findViewById(R.id.show_notifications_list);

        adapter = new ShowNotificationListAdapter<>(c, R.layout.show_notification_item, notifications);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        GeopostNotification notification = adapter.getItem(position);
        ConversationGeom conversation = new ConversationGeom();
        conversation.ConvGuid = notification.ConversationId;
        conversation.Title = notification.Title;
        conversation.Lat = notification.Lat;
        conversation.Lon = notification.Lon;
        ArrayList<ConversationGeom> conversations = new ArrayList<ConversationGeom>();
        conversations.add(conversation);

        LatLng latLng = new LatLng(notification.Lat, notification.Lon);
        GeopostMapFragment fragment = (GeopostMapFragment) c.getSupportFragmentManager().findFragmentById(R.id.map);
        fragment.zoomToLatLng(latLng);
        fragment.controlMarker(conversations);
    }
}
