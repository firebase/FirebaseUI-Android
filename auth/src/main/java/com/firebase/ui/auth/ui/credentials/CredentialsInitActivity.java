package com.firebase.ui.auth.ui.credentials;

import android.content.Context;
import android.content.Intent;

import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.credentials.CredentialsController;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;

import java.util.ArrayList;

/**
 * Created by serikb on 4/21/16.
 */
public class CredentialsInitActivity extends CredentialsBaseActivity {
    public static Intent createIntent(
            Context context, String appName, ArrayList<IDPProviderParcel> parcels) {
        return new Intent()
                .setClass(context, CredentialsInitActivity.class)
                .putExtra(EXTRA_ID, CredentialsController.ID_INIT)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
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
