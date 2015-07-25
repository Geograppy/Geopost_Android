package com.geograppy.geopost;

import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.geograppy.geopost.R;
import com.geograppy.geopost.classes.GetConversationsWithinBufferAsync;
import com.geograppy.geopost.classes.GetUsernameAsync;
import com.geograppy.geopost.classes.GetUsernameTask;
import com.geograppy.geopost.classes.HelpDialog;
import com.geograppy.geopost.classes.Helpers;
import com.geograppy.geopost.classes.OnExceptionThrown;
import com.geograppy.geopost.classes.OnUserIdFetched;
import com.geograppy.geopost.classes.OnUserNameFetched;
import com.geograppy.geopost.classes.OnUsernameEntered;
import com.geograppy.geopost.classes.OnUsernameInDbSet;
import com.geograppy.geopost.classes.SetUsernameDialog;
import com.geograppy.geopost.classes.SetUsernameInDbAsync;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.geograppy.geopost.R.drawable.ic_drawer;

public class MainActivity extends ActionBarActivity implements OnUserNameFetched, OnUsernameInDbSet, OnUserIdFetched, OnUsernameEntered, OnExceptionThrown {
    private static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1002;
    private DrawerLayout mDrawerLayout = null;
    private ListView mDrawerList = null;
    private String[] mDrawerItems;
    private ActionBarDrawerToggle mDrawerToggle = null;
    String mEmail; // Received from newChooseAccountIntent(); passed to getToken()
    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1001;
    private static final String SCOPE =
            "oauth2:https://www.googleapis.com/auth/userinfo.profile";
    private SharedPreferences mSettings;
    private String mUsername;
    private int mNotificationsCount;


//this is a test for the new branch
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSettings = getPreferences(0);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerItems = getResources().getStringArray(R.array.nav_drawer_items);
        putUsernameInDrawer();
        setStatusBarColor(getResources().getColor(R.color.actionbar));
        //mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerList.setAdapter(new ArrayAdapter<String>(
                this, R.layout.drawer_list_item, mDrawerItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener(this));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerOpened(View view) {
                invalidateOptionsMenu();
            }

            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        if (!Helpers.hasKnownUsername(this)) getUsername();

        loadAdBanner();
    }

    private void loadAdBanner(){
        AdView mAdView = (AdView) findViewById(R.id.adView);

        AdRequest adRequest = new AdRequest.Builder()
                //.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mAdView.loadAd(adRequest);
    }

    public void refreshDrawerItem(){
        mDrawerItems = getResources().getStringArray(R.array.nav_drawer_items);
        putUsernameInDrawer();
        mDrawerList.setAdapter(new ArrayAdapter<String>(
                this, R.layout.drawer_list_item, mDrawerItems));
    }

    private void putUsernameInDrawer(){
        mDrawerItems[0] = mDrawerItems[0] + " " + Helpers.getUsernameFromPreferences(this);
    }

    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    public void setStatusBarColor(int color){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(color);
        }
        getSupportActionBar().setElevation(0);

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == RESULT_OK) {
                mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                // With the account name acquired, go get the auth token
                getUsername();
            } else if (resultCode == RESULT_CANCELED) {
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Toast.makeText(this, R.string.pick_account, Toast.LENGTH_SHORT).show();
                pickUserAccount();
            }
        } else if ((requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR ||
                requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR)
                && resultCode == RESULT_OK) {
            // Receiving a result that follows a GoogleAuthException, try auth again
            getUsername();
        }
    }

    private void getUsername() {
        if (mEmail == null) {
            pickUserAccount();
        } else {
            if (isDeviceOnline()) {
                new GetUsernameTask(this, mEmail, SCOPE, this, this).execute();
            } else {
                Toast.makeText(this, R.string.not_online, Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isDeviceOnline(){
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);

        for (int index = 0; index < menu.size(); index++) {
            MenuItem menuItem = menu.getItem(index);
            if (menuItem != null) {
                // hide the menu items if the drawer is open
                menuItem.setVisible(!drawerOpen);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.global, menu);

        MenuItem item = menu.findItem(R.id.action_notifications);
        LayerDrawable icon = (LayerDrawable) item.getIcon();

        Helpers.setBadgeCount(this, icon, mNotificationsCount);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == R.id.action_notifications)
        {
            GeopostMapFragment fragment = (GeopostMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            fragment.showNotificationList();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateNotificationsBadge(int count){
        mNotificationsCount = count;
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onUserIdFetched(int userId) {
        String userIdString = Integer.toString(userId);
        new GetUsernameAsync(this, this).execute(userIdString);
    }

    @Override
    public void onUserNameFetched(String userName){
        if (userName.equals("-1") || userName.equals("")){
            showSetUsernameDialog();
        }
        else {
            Helpers.setUsernameInPreferences(userName, this);
            refreshDrawerItem();
        }
    }

    @Override
    public void onUsernameEntered(String username) {

        if (!username.isEmpty()) {
            mUsername = username;
            new SetUsernameInDbAsync(this, this).execute(username);
            refreshDrawerItem();
        }
        else showSetUsernameDialog();
    }

    private void showSetUsernameDialog(){
        final Dialog dialog = new SetUsernameDialog(this, this);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().getAttributes().windowAnimations = R.style.add_geopost_dialog_animation;

        dialog.show();
    }

    @Override
    public void onUsernameInDbSet(String username) {
        if (mUsername.equals(username)) {
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT);
            Helpers.setUsernameInPreferences(username, this);
            refreshDrawerItem();
        }
        else if (username.equals("-1")){
            Toast.makeText(this, R.string.UsernameExists, Toast.LENGTH_SHORT).show();
            showSetUsernameDialog();
        }
        else showSetUsernameDialog();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        private Activity a;

        public DrawerItemClickListener(Activity mainActivity) {
            this.a = mainActivity;
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            switch (position) {
                case 0: {
                    showSetUsernameDialog();
                }
                case 1: {
                    GeopostMapFragment fragment = (GeopostMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    fragment.goToMyLocation();
                    break;
                }
                case 2: {
                    GeopostMapFragment fragment = (GeopostMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    fragment.postToMyLocation();
                    break;
                }
                case 3: {
                    final Dialog dialog = new HelpDialog(this.a);
                    dialog.getWindow().setGravity(Gravity.BOTTOM);
                    dialog.getWindow().getAttributes().windowAnimations = R.style.add_geopost_dialog_animation;

                    dialog.show();
                    break;
                }
                case 4: {
                    Helpers.setUsernameInPreferences("", this.a);
                    Helpers.setUseridInPreferences(-1, this.a);
                    finish();
                    startActivity(getIntent());
                    break;
                }
                default:
                    break;
            }
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    protected void onNewIntent(Intent intent){
        GeopostMapFragment fragment = (GeopostMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        fragment.goToMyLocation();
    }


    @Override
    public void onExceptionThrown(final Exception e) {
        // Because this call comes from the AsyncTask, we must ensure that the following
        // code instead executes on the UI thread.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException)e)
                            .getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                            MainActivity.this,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException)e).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }

}