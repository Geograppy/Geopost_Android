package com.geograppy.geopost.classes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.geograppy.geopost.R;

import java.util.List;

/**
 * Created by benito on 04/05/15.
 */

public class ShowNotificationListAdapter<G> extends ArrayAdapter<GeopostNotification> {
    private Context context;
    private List<GeopostNotification> notifications;

    public ShowNotificationListAdapter(Context context, int resource, List<GeopostNotification> objects) {
        super(context, resource, objects);
        this.context = context;
        this.notifications = objects;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = (View) inflater.inflate(
                    R.layout.show_notification_item, null);
        }

        TextView text=(TextView)convertView.findViewById(R.id.show_notification_item_text);

        if (notifications.get(position).NotificationType == 1){
            text.setText(context.getString(R.string.notifications_list_conversation_found) + notifications.get(position).Title);
        }
        else{
            text.setText(context.getString(R.string.notifications_list_comment) + notifications.get(position).Title);
        }
        return convertView;
    }


}
