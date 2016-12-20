package com.firebase.ui.auth;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;

import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.util.signincontainer.SignInDelegate;
import com.google.android.gms.common.GoogleApiAvailability;

public class KickoffActivity extends AppCompatBase {
    private static final String TAG = "KickoffActivity";
    private static final int RC_PLAY_SERVICES = 1;

    private boolean mIsWaitingForPlayServices = false;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        if (savedInstance == null || !savedInstance.getBoolean(ExtraConstants.HAS_EXISTING_INSTANCE)) {
            if (!hasNetworkConnection()) {
                Log.d(TAG, "No network connection");
                finish(ErrorCodes.NO_NETWORK, IdpResponse.getErrorCodeIntent(ErrorCodes.NO_NETWORK));
                return;
            }

            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            Dialog errorDialog = apiAvailability.getErrorDialog(
                    this,
                    apiAvailability.isGooglePlayServicesAvailable(this),
                    RC_PLAY_SERVICES,
                    new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish(ResultCodes.CANCELED, new Intent());
                        }
                    });

            // The error dialog will be null if isGooglePlayServicesAvailable returned SUCCESS
            if (errorDialog == null) {
                SignInDelegate.delegate(KickoffActivity.this,
                                        mActivityHelper.getFlowParams());
            } else {
                errorDialog.show();
                mIsWaitingForPlayServices = true;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // It doesn't matter what we put here, we just don't want outState to be empty
        outState.putBoolean(ExtraConstants.HAS_EXISTING_INSTANCE, !mIsWaitingForPlayServices);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PLAY_SERVICES) {
            if (resultCode == ResultCodes.OK) {
                SignInDelegate.delegate(KickoffActivity.this,
                                        mActivityHelper.getFlowParams());
            } else {
                finish(ResultCodes.CANCELED,
                       IdpResponse.getErrorCodeIntent(ErrorCodes.UNKNOWN_ERROR));
            }
        } else {
            SignInDelegate delegate = SignInDelegate.getInstance(this);
            if (delegate != null) delegate.onActivityResult(requestCode, resultCode, data);
        }
    }


    /**
     * Check if there is an active or soon-to-be-active network connection.
     */
    private boolean hasNetworkConnection() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return manager != null
                && manager.getActiveNetworkInfo() != null
                && manager.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return ActivityHelper.createBaseIntent(context, KickoffActivity.class, flowParams);
    }
}
