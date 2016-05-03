package com.firebase.ui.auth.ui.credentials;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.credentials.CredentialsController;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.util.ArrayList;

/**
 * Created by serikb on 4/21/16.
 */
public class CredentialsInitActivity extends CredentialsBaseActivity {
    private static final String TAG = "CredentialsInitActivity";

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        try{
            FirebaseApp.getInstance(ControllerConstants.APP_NAME);
            return;
        } catch (IllegalStateException e) {
            Log.d(TAG, "FirebaseApp is not created yet");
        }

        String apiaryKey = getIntent().getStringExtra(ControllerConstants.EXTRA_APIARY_KEY);
        String applicationId = getIntent().getStringExtra(ControllerConstants.EXTRA_APPLICATION_ID);
        String appName = getIntent().getStringExtra(ControllerConstants.EXTRA_APP_NAME);

        FirebaseOptions options
                = new FirebaseOptions.Builder()
                .setApiKey(apiaryKey)
                .setApplicationId(applicationId)
                .build();

        FirebaseApp.initializeApp(this, options, appName);
    }

    public static Intent createIntent(
            Context context, String appName, ArrayList<IDPProviderParcel> parcels, String
            apiaryKey, String applicationId) {
        return new Intent()
                .setClass(context, CredentialsInitActivity.class)
                .putExtra(EXTRA_ID, CredentialsController.ID_INIT)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
                .putExtra(ControllerConstants.EXTRA_APIARY_KEY, apiaryKey)
                .putExtra(ControllerConstants.EXTRA_APPLICATION_ID, applicationId)
                .putParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS, parcels);
    }

    @Override
    protected Controller setUpController() {
        super.setUpController();
        return new CredentialsController(getApplicationContext(), mCredentialsAPI, mAppName);
    }

    @Override
    public void asyncTasksDone() {
        finish(RESULT_OK, getIntent());
    }
}
