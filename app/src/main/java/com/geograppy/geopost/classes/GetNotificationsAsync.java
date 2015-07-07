package com.geograppy.geopost.classes;

import android.content.Context;
import android.os.AsyncTask;

import com.geograppy.geopost.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by benito on 25/04/15.
 */
public class GetNotificationsAsync extends AsyncTask<String, Integer, ArrayList<GeopostNotification>> {

    private Context mContext;
    private int count = 0;
    private OnNotifcationsReceived listener;
    public ArrayList<ConversationGeom> mConversations;
    private boolean firstConverstions = true;
    public GetNotificationsAsync(Context context, OnNotifcationsReceived listener){
        mContext = context;
        this.listener = listener;
    }

    @Override
    protected ArrayList<GeopostNotification> doInBackground(String... params) {
        // TODO Auto-generated method stub
        return postData(params[0]);
        //postDataManyTimes(params[0]);
        //return null;
    }

    protected void onPostExecute(ArrayList<GeopostNotification> result){
        //pb.setVisibility(View.GONE);
        //double showRestult = result;
        this.listener.onNotifcationsReceived(result);
    }

    private ArrayList<GeopostNotification> getNotificationListFromJson(String json){

        ArrayList<GeopostNotification> notifications = new ArrayList<GeopostNotification>();
        GeopostNotification notification = null;
        JSONObject jObj = null;
        try {
            jObj = new JSONObject(json);

            JSONArray jArr = jObj.getJSONArray("Notifications");
            for (int i=0; i < jArr.length(); i++) {
                JSONObject obj = jArr.getJSONObject(i);
                notification = new GeopostNotification();
                notification.ConversationId = UUID.fromString(obj.getString("ConvGuid"));
                notification.Lat = obj.getDouble("Lat");
                notification.Lon = obj.getDouble("Lon");
                notification.Title = obj.getString("Title");
                notification.NotificationType = obj.getInt("NotificationType");
                notifications.add(notification);
            }
            return notifications;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<GeopostNotification> postData(String value){
        URL url = null;
        HttpURLConnection conn = null;
        try {
            url = new URL(mContext.getString(R.string.wsUrl) + "/get/notifications");
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            //OutputStream os = conn.getOutputStream();
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(value);
            os.flush();
            os.close();

            // get response
            int responseHttpCode = conn.getResponseCode();
            InputStream _is;
            if (responseHttpCode / 100 == 2) { // 2xx code means success
                _is = conn.getInputStream();
                count = 0;
            } else {
                count++;
                if (count < 4) postData(value);
                _is = conn.getErrorStream();

                //String result = getStringFromInputStream(_is);
                //Log.i("Error != 2xx", result);
            }
            //= new BufferedInputStream(conn.getInputStream());
            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(_is));
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            responseStreamReader.close();

            return getNotificationListFromJson(stringBuilder.toString());



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
        return null;
    }

}