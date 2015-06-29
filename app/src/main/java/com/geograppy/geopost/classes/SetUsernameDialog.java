package com.geograppy.geopost.classes;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.geograppy.geopost.R;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONStringer;

public class SetUsernameDialog extends Dialog implements View.OnClickListener {

    public Activity c;
    public Dialog d;
    public Button submit;
    public EditText editText;
    private OnUsernameEntered mListener;

    public SetUsernameDialog(Activity a, OnUsernameEntered listener) {
        super(a);
        this.mListener = listener;
        this.c = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.set_username_dialog);
        editText = (EditText) findViewById(R.id.set_username_edit);
        submit = (Button) findViewById(R.id.btn_submit);
        //no = (Button) findViewById(R.id.btn_no);
        submit.setOnClickListener(this);
        //no.setOnClickListener(this);
        setEditTextFilter();
    }

    protected void setEditTextFilter(){
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(20);
        editText.setFilters(filterArray);
        editText.setHorizontallyScrolling(false);
        editText.setSingleLine(true);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_submit:
                mListener.onUsernameEntered(editText.getText().toString());


                dismiss();
                break;

            default:
                break;
        }
        dismiss();
    }


}