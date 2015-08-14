package com.geograppy.geopost.classes;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.geograppy.geopost.R;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

/**
 * Created by benito on 29/04/15.
 */
public class ShowGeopostFromMarker extends Dialog implements OnAnswerGeopostCompleted, OnGetConversationsByIdCompleted,
        android.view.View.OnClickListener {
    public Activity c;
    public Dialog d;
    public Button comment;
    public UUID geopostId;
    private LatLng latLng;
    private String title;
    private ShowGeopostListAdapter<Geopost> adapter;
    private Boolean initialLoad = true;

    public ShowGeopostFromMarker(Activity a, ConversationGeom conversation) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
        //this.latLng = new LatLng(conversation.Lat, conversation.Lon);
        this.geopostId = conversation.ConvGuid;
        this.title = conversation.Title;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.show_post_dialog);
        String jsonObject = getConversationsJson();
        new GetConversationsByIdAsync(c, this).execute(jsonObject);
        TextView titleView = (TextView) findViewById(R.id.show_geopost_title);
        titleView.setText(this.title);
        comment = (Button) findViewById(R.id.btn_comment);
        //no = (Button) findViewById(R.id.btn_no);
        comment.setOnClickListener(this);
    }

    private String getConversationsJson() {

        // Add your data
        JSONStringer post = null;
        try {
            post = new JSONStringer()
                    .object()
                    .key("getConversationByIdRequest")
                    .object()
                    .key("ConvGuid").value(geopostId.toString())
                    .endObject()
                    .endObject();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return post.toString();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_comment:
                //String entity = getJson();
                //ew AddGeopostAsync(c).execute(entity);
                showAnswerDialog();
                break;
            case R.id.btn_answer:
                hideSoftKeyboard();
                String entity = getJson();
                Toast.makeText(c, "Sending...", Toast.LENGTH_SHORT).show();
                new AnswerGeopostAsync(c, this).execute(entity);
                hideAnswerDialog();
                scrollDownListView();
                break;

            default:
                break;
        }
        //dismiss();
    }

    public String getJson() {
        EditText myTextView = (EditText) findViewById(R.id.answer_geopost_edit);

        // Add your data
        JSONStringer post = null;
        try {
            post = new JSONStringer()
                    .object()
                    .key("answerGeopostRequest")
                    .object()
                    .key("Text").value(myTextView.getText())
                    .key("UserId").value(Helpers.getUseridFromPreferences(c))
                    .key("ConvGuid").value(geopostId.toString())
                    .endObject()
                    .endObject();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return post.toString();

    }

    private void showAnswerDialog(){
        RelativeLayout hiddenLayout = (RelativeLayout)findViewById(R.id.answer_geopost_dialog);
        if(hiddenLayout == null){
            //Inflate the Hidden Layout Information View
            LinearLayout myLayout = (LinearLayout)findViewById(R.id.show_geopost_dialog);
            View hiddenInfo = getLayoutInflater().inflate(R.layout.answer_geopost_dialog, myLayout, false);
            myLayout.addView(hiddenInfo);
        }
        View button = findViewById(R.id.btn_comment);
        button.setVisibility(View.GONE);
        //Get References to the TextView
        EditText editText = (EditText) findViewById(R.id.answer_geopost_edit);

        Button answer = (Button) findViewById(R.id.btn_answer);
        //no = (Button) findViewById(R.id.btn_no);
        answer.setOnClickListener(this);
    }
    private void hideSoftKeyboard(){
        InputMethodManager imm = (InputMethodManager) c.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    private void hideAnswerDialog(){
        View button = findViewById(R.id.btn_comment);
        button.setVisibility(View.VISIBLE);
        View hiddenLayout = findViewById(R.id.answer_geopost_dialog);
        ViewGroup parent = (ViewGroup) hiddenLayout.getParent();

        parent.removeView(hiddenLayout);
    }

    @Override
    public void OnGetConversationsByIdCompleted(ArrayList<Geopost> geopost) {
        if (geopost == null) {
            this.dismiss();
            return;
        }
        Collections.sort(geopost, new customComparator());
        ListView listView = (ListView) findViewById(R.id.show_geopost_list);

        adapter = new ShowGeopostListAdapter<>(c, R.layout.show_geopost_item, geopost);
        listView.setAdapter(adapter);
        //if (!this.title.isEmpty())titleView.setText((CharSequence) this.title);
        if (!initialLoad){
            scrollDownListView();
        }
        initialLoad = false;
    }

    private ArrayList<String> getTextFromGeopostList(ArrayList<Geopost> geopost){
        ArrayList<String> textList = new ArrayList<String>();
        for (int ii = 0; ii<geopost.size(); ii++){
            textList.add(geopost.get(ii).Text);
        }
        return textList;
    }

    @Override
    public void onAnswerGeopostCompleted() {
        String jsonObject = getConversationsJson();
        new GetConversationsByIdAsync(c, this).execute(jsonObject);

    }

    private void scrollDownListView(){
        final ListView listView = (ListView) findViewById(R.id.show_geopost_list);
        listView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                listView.setSelection(adapter.getCount() - 1);
            }
        });
    }

    class customComparator implements Comparator<Geopost> {
        @Override
        public int compare(Geopost lhs, Geopost rhs) {
            return rhs.Sorter.compareTo(lhs.Sorter);
        }
    }
}
