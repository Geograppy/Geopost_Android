package com.geograppy.geopost.classes;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.geograppy.geopost.R;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONStringer;

public class CustomDialogClass extends Dialog implements OnGeopostCompleted,
        android.view.View.OnClickListener {

    public Activity c;
    public Dialog d;
    public Button post;
    public EditText editText;
    public EditText titleText;
    private LatLng latLng;
    private OnTaskCompleted getConversationsByBufferListener;

    public CustomDialogClass(Activity a, OnTaskCompleted listener, LatLng latLng) {
        super(a);

        this.c = a;
        this.latLng = latLng;
        this.getConversationsByBufferListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.add_post_dialog);
        editText = (EditText) findViewById(R.id.add_geopost_edit);
        titleText = (EditText) findViewById(R.id.add_geopost_title);
        post = (Button) findViewById(R.id.btn_post);
        //no = (Button) findViewById(R.id.btn_no);
        post.setOnClickListener(this);
        //no.setOnClickListener(this);
        setEditTextFilter();
    }

    protected void setEditTextFilter(){
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(60);
        editText.setFilters(filterArray);
        editText.setHorizontallyScrolling(false);
        editText.setSingleLine(false);

        InputFilter[] filterArraytitle = new InputFilter[1];
        filterArraytitle[0] = new InputFilter.LengthFilter(25);
        titleText.setFilters(filterArray);
        titleText.setHorizontallyScrolling(false);
        titleText.setSingleLine(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_post:
                try {
                    String entity = getJson();
                    new AddGeopostAsync(c, this).execute(entity);
                    Toast.makeText(c, "Sending...", Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                dismiss();
                break;

            default:
                break;
        }
        dismiss();
    }

    public String getJson() throws JSONException {

        // Add your data
        JSONStringer post = new JSONStringer()
                .object()
                .key("addGeopostRequest")
                .object()
                .key("Title").value(titleText.getText())
                .key("Text").value(editText.getText())
                .key("UserId").value(Helpers.getUseridFromPreferences(c))
                .key("Lon").value(latLng.longitude)
                .key("Lat").value(latLng.latitude)
                .endObject()
                .endObject();


        return post.toString();

    }

    @Override
    public void onGeopostCompleted() {
        String entity = null;
        try {
            entity = getConversationsJson();
            new GetConversationsWithinBufferAsync(c, getConversationsByBufferListener).execute(entity);
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
                .key("Buffer").value(Helpers.getBufferDistance(c))
                .key("Lon").value(latLng.longitude)
                .key("Lat").value(latLng.latitude)
                .endObject()
                .endObject();


        return post.toString();
    }
}