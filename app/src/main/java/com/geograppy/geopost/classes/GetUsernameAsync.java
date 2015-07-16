package com.geograppy.geopost.classes;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.geograppy.geopost.R;

import org.json.JSONException;
import org.json.JSONStringer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by benito on 25/04/15.
 */
public class GetUsernameAsync extends AsyncTask<String, Integer, String> {

    private Activity mActivity;
    private OnUserNameFetched listener;
    private int count = 0;
    public GetUsernameAsync(Activity activity, OnUserNameFetched listener){
        mActivity = activity;
        this.listener = listener;
    }

    protected void onPostExecute(String result){
        //pb.setVisibility(View.GONE);
        //double showRestult = result;
        if (result.isEmpty() || result == null) result = "-1";
        listener.onUserNameFetched(result);
        //Toast.makeText(mContext, "Geoposted", Toast.LENGTH_LONG).show();
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            return postData(params[0]);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "-1";
    }

    public String postData(String userid) throws JSONException {
        URL url = null;
        HttpURLConnection conn = null;
        try {
            String urlString = mActivity.getString(R.string.wsUrl) + "/get/username?userid="+userid;
            url = new URL(urlString);

            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(3000);
            conn.setConnectTimeout(6000);
            conn.setRequestMethod("GET");

            // get response
            int responseHttpCode = conn.getResponseCode();
            InputStream _is;
            if (responseHttpCode / 100 == 2) { // 2xx code means success
                _is = conn.getInputStream();
                count =0;
            } else {
                count++;
                if (count < 4) postData(userid);
                //else Toast.makeText(mActivity, R.string.getUsernameFailed, Toast.LENGTH_SHORT).show();
                _is = conn.getErrorStream();

            }

            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(_is));
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            responseStreamReader.close();
            String output = stringBuilder.toString().replace("\"", "");
            return output;



        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally{
            conn.disconnect();
        }
        return "-1";
    }


    private String buildSetUsernameJson(String username) throws JSONException {
        // Add your data
        JSONStringer post = new JSONStringer()
                .object()
                .key("setGeopostUsernameRequest")
                .object()
                .key("Username").value(username)
                .key("UserId").value(Helpers.getUseridFromPreferences(mActivity))
                .endObject()
                .endObject();


        return post.toString();
    }

}