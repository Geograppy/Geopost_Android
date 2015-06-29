package com.geograppy.geopost.classes;

import android.app.Activity;
import android.os.AsyncTask;

import com.geograppy.geopost.MainActivity;
import com.geograppy.geopost.R;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

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
 * Created by benito on 18/05/15.
 */
public class GetUsernameTask extends AsyncTask<String, Integer, Integer> {
    Activity mActivity;
    String mScope;
    String mEmail;
    private int loginCount = 0;
    private Exception exceptionToBeThrown;
    private OnExceptionThrown mListener;
    private OnUserIdFetched mUserIdListner;

    public GetUsernameTask(MainActivity activity, String name, String scope, OnExceptionThrown listener, OnUserIdFetched onUserIdFetched) {
        this.mActivity = activity;
        this.mScope = scope;
        this.mEmail = name;
        this.mListener = listener;
        this.mUserIdListner = onUserIdFetched;
    }

    protected String fetchToken() throws IOException {
        try {
            return GoogleAuthUtil.getToken(mActivity, mEmail, mScope);
        } catch (UserRecoverableAuthException userRecoverableException) {
            // GooglePlayServices.apk is either old, disabled, or not present
            // so we need to show the user some UI in the activity to recover.
            mListener.onExceptionThrown(userRecoverableException);
        } catch (GoogleAuthException fatalException) {
            // Some other type of unrecoverable exception has occurred.
            // Report and log the error as appropriate for your app.

        }
        return null;
    }

    @Override
    protected void onPostExecute(Integer result){

        Helpers.setUseridInPreferences(result, mActivity);
        mUserIdListner.onUserIdFetched(result);
    }



    @Override
    protected Integer doInBackground(String... params) {
        String token = null;
        try {
            token = fetchToken();
            if (token != null) {
                int userId = login(token);
                if (userId != -1) {
                    return userId;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }



    public int login(String token) throws JSONException {
        URL url = null;
        HttpURLConnection conn = null;
        try {
            String urlString = mActivity.getString(R.string.wsUrl) + "/login";
            url = new URL(urlString);

            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(30000);
            conn.setConnectTimeout(40000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            String requestValue = buildLoginJson(token);
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
                loginCount =0;
            } else {
                loginCount++;
                if (loginCount < 4) login(token);
                _is = conn.getErrorStream();

            }

            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(_is));
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            responseStreamReader.close();

            return getUserIdFromJson(stringBuilder.toString());

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
        return -1;
    }

    private int getUserIdFromJson(String json) throws JSONException {
        //SONObject jObj = new JSONObject(json);
        int value = Integer.parseInt(json);
        return value;
    }

    private String buildLoginJson(String token)throws JSONException {

        // Add your data
        JSONStringer post = new JSONStringer()
                .object()
                .key("loginGeopostUserRequest")
                .object()
                .key("Token").value(token)
                .key("Email").value(mEmail)
                .endObject()
                .endObject();


        return post.toString();
    }


}
