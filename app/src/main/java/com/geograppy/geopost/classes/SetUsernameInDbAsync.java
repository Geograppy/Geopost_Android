package com.geograppy.geopost.classes;

import android.app.Activity;
import android.os.AsyncTask;

import com.geograppy.geopost.R;

import org.json.JSONException;
import org.json.JSONObject;
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
public class SetUsernameInDbAsync extends AsyncTask<String, Integer, String> {

    private Activity mActivity;
    private OnUsernameInDbSet listener;
    private int count = 0;
    public SetUsernameInDbAsync(Activity activity, OnUsernameInDbSet listener){
        mActivity = activity;
        this.listener = listener;
    }

    protected void onPostExecute(String result){
        //pb.setVisibility(View.GONE);
        //double showRestult = result;
        listener.onUsernameInDbSet(result);
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

    public String postData(String username) throws JSONException {
        URL url = null;
        HttpURLConnection conn = null;
        try {
            String urlString = mActivity.getString(R.string.wsUrl) + "/set/username";
            url = new URL(urlString);

            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            String requestValue = buildSetUsernameJson(username);
            //OutputStream os = conn.getOutputStream();
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(requestValue);
            os.flush();
            os.close();

            // get response
            int responseHttpCode = conn.getResponseCode();
            InputStream _is;
            if (responseHttpCode / 100 == 2) { // 2xx code means success
                _is = conn.getInputStream();
                count =0;
            } else {
                count++;
                if (count < 4) postData(username);
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