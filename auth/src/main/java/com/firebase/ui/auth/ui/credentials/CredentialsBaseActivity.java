package com.firebase.ui.auth.ui.credentials;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.firebase.ui.auth.api.CredentialsAPI;
import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.ui.BaseActivity;

import java.util.ArrayList;

/**
 * Created by serikb on 4/26/16.
 */
public abstract class CredentialsBaseActivity extends BaseActivity {
    protected CredentialsAPI mCredentialsAPI;
    private ArrayList<Parcelable> mParcelables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParcelables = getIntent().getParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS);
    }

    @Override
    public void finish(int resultCode, Intent data) {
        data.putParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS, mParcelables);
        super.finish(resultCode, data);
    }

    @Override
    protected Controller setUpController() {
        mCredentialsAPI = new CredentialsAPI(this);
        return null;
    }

    /**
     * Override this method to handle async tasks. I.E.: if you need to wait until asyncTask(s)
     * will be done processing.
     */
    public void asyncTasksDone() {
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCredentialsAPI.isGoogleApiClient()){
            mCredentialsAPI.getGoogleApiClient().connect();
        }
    }

    @Override
    protected void onStop() {
        if (mCredentialsAPI.isGoogleApiClient() && mCredentialsAPI.getGoogleApiClient().isConnected()) {
            mCredentialsAPI.getGoogleApiClient().disconnect();
        }
        super.onStop();
    }

}
