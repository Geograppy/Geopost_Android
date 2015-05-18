package com.geograppy.geopost.classes;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

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
public class AnswerGeopostAsync extends AsyncTask<String, Integer, Double> {

    private Context mContext;
    private OnAnswerGeopostCompleted listener;
    private int count = 0;
    public AnswerGeopostAsync(Context context, OnAnswerGeopostCompleted listener){
        mContext = context;
        this.listener = listener;
    }

    @Override
    protected Double doInBackground(String... params) {
        // TODO Auto-generated method stub
        postData(params[0]);
        //postDataManyTimes(params[0]);
        return null;
    }

    protected void onPostExecute(Double result){
        //pb.setVisibility(View.GONE);
        //double showRestult = result;
        listener.onAnswerGeopostCompleted();
        Toast.makeText(mContext, "Geoposted", Toast.LENGTH_LONG).show();
    }

    public void postData(String value){
        URL url = null;
        HttpURLConnection conn = null;
        try {
            url = new URL("http://geopostwsdev.azurewebsites.net/Service.svc/answer/geopost");

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
                count =0;
            } else {
                count++;
                if (count < 4) postData(value);
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
    }

}