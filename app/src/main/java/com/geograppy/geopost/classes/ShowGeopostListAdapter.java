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

public class ShowGeopostListAdapter<G> extends ArrayAdapter<Geopost> {
    private Context context;
    private List<Geopost> geoposts;

    public ShowGeopostListAdapter(Context context, int resource, List<Geopost> objects) {
        super(context, resource, objects);
        this.context = context;
        this.geoposts = objects;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = (View) inflater.inflate(
                    R.layout.show_geopost_item, null);
        }

        TextView username = (TextView)convertView.findViewById(R.id.show_geopost_item_username);
        TextView text=(TextView)convertView.findViewById(R.id.show_geopost_item_text);
        TextView timeago = (TextView)convertView.findViewById(R.id.show_geopost_item_timeago);

        username.setText(geoposts.get(position).UserName + " " + context.getString(R.string.said) + ":");
        text.setText(geoposts.get(position).Text);
        timeago.setText("posted " + geoposts.get(position).TimeAgo);

        return convertView;
    }


}
