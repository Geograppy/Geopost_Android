package com.geograppy.geopost.classes;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.geograppy.geopost.R;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by benito on 25/04/15.
 */
public class AnswerGeopostAsync extends AsyncTask<String, Integer, Boolean> {

    private Context mContext;
    private OnAnswerGeopostCompleted listener;
    private int count = 0;
    public AnswerGeopostAsync(Context context, OnAnswerGeopostCompleted listener){
        mContext = context;
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        // TODO Auto-generated method stub

        //postDataManyTimes(params[0]);
        return postData(params[0]);
    }

    protected void onPostExecute(Boolean result){
        //pb.setVisibility(View.GONE);
        //double showRestult = result;
        Toast.makeText(mContext, "Sending...", Toast.LENGTH_SHORT).cancel();
        listener.onAnswerGeopostCompleted();
        if (result) Toast.makeText(mContext, "Geoposted", Toast.LENGTH_SHORT).show();
        else Toast.makeText(mContext, R.string.postFailed, Toast.LENGTH_SHORT).show();
    }

    public Boolean postData(String value){
        URL url = null;
        HttpURLConnection conn = null;
        Boolean result = false;
        try {
            url = new URL(mContext.getString(R.string.wsUrl) + "/answer/geopost");

            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(6000);
            conn.setConnectTimeout(10000);
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
                count =0;
                result = true;
            } else {
                count++;
                if (count < 4) postData(value);
                else result = false;
                _is = conn.getErrorStream();


                //String result = getStringFromInputStream(_is);
                //Log.i("Error != 2xx", result);
            }
            /*//= new BufferedInputStream(conn.getInputStream());
            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(_is));
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            responseStreamReader.close();

            String response = stringBuilder.toString();*/

            conn.disconnect();
            return result;

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
        return result;
    }

}