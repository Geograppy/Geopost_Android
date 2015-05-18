package com.geograppy.geopost.classes;

import android.app.Activity;
import android.util.TypedValue;

import com.geograppy.geopost.R;

public class Helpers {
    public static int getBufferDistance(Activity a){
        return a.getResources().getInteger(R.integer.buffer_for_conversations);
    }
}
