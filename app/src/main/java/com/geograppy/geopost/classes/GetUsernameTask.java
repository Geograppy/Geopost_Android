package com.geograppy.geopost.classes;

import android.app.Activity;
import android.os.AsyncTask;

import com.geograppy.geopost.MainActivity;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.IOException;

/**
 * Created by benito on 18/05/15.
 */
public class GetUsernameTask extends AsyncTask {
    Activity mActivity;
    String mScope;
    String mEmail;
    private Exception exceptionToBeThrown;
    private OnExceptionThrown mListener;

    public GetUsernameTask(MainActivity activity, String name, String scope, OnExceptionThrown listener) {
        this.mActivity = activity;
        this.mScope = scope;
        this.mEmail = name;
        this.mListener = listener;
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
    protected Object doInBackground(Object[] params) {
        try {
            String token = fetchToken();
            if (token != null) {
            //this.mActivity.mSettings =

            }
        } catch (IOException e) {
            // The fetchToken() method handles Google-specific exceptions,
            // so this indicates something went wrong at a higher level.
            // TIP: Check for network connectivity before starting the AsyncTask.
        }
        return null;
    }
}
