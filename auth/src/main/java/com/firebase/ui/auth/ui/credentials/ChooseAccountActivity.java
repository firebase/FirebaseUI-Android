package com.firebase.ui.auth.ui.credentials;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.credentials.Credential;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.credentials.CredentialsController;

/**
 * Created by serikb on 4/21/16.
 */
public class ChooseAccountActivity extends CredentialsBaseActivity{
    private static final String TAG = "ChooseAccountActivity";
    private static final int RC_CREDENTIALS_READ = 2;

    @Override
    protected Controller setUpController() {
        super.setUpController();
        return new CredentialsController(this, mCredentialsAPI, mAppName);
    }

    @Override
    public void asyncTasksDone() {
        mCredentialsAPI.resolveSavedEmails(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
        }

        if (requestCode == RC_CREDENTIALS_READ) {
            if (resultCode == RESULT_OK) {
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                mCredentialsAPI.handleCredential(credential);
                mCredentialsAPI.resolveSignIn();
                finish(RESULT_OK, getIntent());
            } else if (resultCode == RESULT_CANCELED) {
                finish(RESULT_OK, getIntent());
            } else if (resultCode == RESULT_FIRST_USER) {
                // TODO: (serikb) figure out flow
            }
        }
    }

}
