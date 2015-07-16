package com.geograppy.geopost.classes;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.geograppy.geopost.GeopostMapFragment;
import com.geograppy.geopost.MainActivity;

public  class TouchableWrapper extends FrameLayout {

    private long lastTouched = 0;
    private static final long SCROLL_TIME = 1000L; // 200 Milliseconds, but you can adjust that to your liking
    private long lastUpdated = 0;
    private static final long UPDATE_INTERVAL = 30000;
    private UpdateMapAfterUserInterection updateMapAfterUserInterection;

    public TouchableWrapper(Context context, UpdateMapAfterUserInterection listener) {
        super(context);
        // Force the host activity to implement the UpdateMapAfterUserInterection Interface
        try {
            updateMapAfterUserInterection = listener;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement UpdateMapAfterUserInterection");
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouched = SystemClock.uptimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                final long now = SystemClock.uptimeMillis();
                if (now - lastTouched > SCROLL_TIME && now - lastUpdated > UPDATE_INTERVAL) {
                    // Update the map
                    lastUpdated = SystemClock.uptimeMillis();
                    updateMapAfterUserInterection.onUpdateMapAfterUserInterection();
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    // Map Activity must implement this interface
    public interface UpdateMapAfterUserInterection {
        public void onUpdateMapAfterUserInterection();
    }
}