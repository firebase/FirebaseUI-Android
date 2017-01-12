package com.firebase.ui.auth;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.util.PlayServicesHelper;
import com.firebase.ui.auth.util.signincontainer.SignInDelegate;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class KickoffActivity extends HelperActivityBase {
    private static final String TAG = "KickoffActivity";
    private static final String IS_WAITING_FOR_PLAY_SERVICES = "is_waiting_for_play_services";
    private static final int RC_PLAY_SERVICES = 1;

    private boolean mIsWaitingForPlayServices = false;

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return ActivityHelper.createBaseIntent(context, KickoffActivity.class, flowParams);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        if (savedInstance == null || savedInstance.getBoolean(IS_WAITING_FOR_PLAY_SERVICES)) {
            if (isOffline()) {
                Log.d(TAG, "No network connection");
                finish(ErrorCodes.NO_NETWORK,
                       IdpResponse.getErrorCodeIntent(ErrorCodes.NO_NETWORK));
                return;
            }

            boolean isPlayServicesAvailable = PlayServicesHelper.makePlayServicesAvailable(
                    this,
                    RC_PLAY_SERVICES,
                    new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish(ResultCodes.CANCELED,
                                   IdpResponse.getErrorCodeIntent(
                                           ErrorCodes.UNKNOWN_ERROR));
                        }
                    });

            if (isPlayServicesAvailable) {
                SignInDelegate.delegate(this, mActivityHelper.getFlowParams());
            } else {
                mIsWaitingForPlayServices = true;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // It doesn't matter what we put here, we just don't want outState to be empty
        outState.putBoolean(ExtraConstants.HAS_EXISTING_INSTANCE, true);
        outState.putBoolean(IS_WAITING_FOR_PLAY_SERVICES, mIsWaitingForPlayServices);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PLAY_SERVICES) {
            if (resultCode == ResultCodes.OK) {
                SignInDelegate.delegate(this, mActivityHelper.getFlowParams());
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
     *
     * @return true if there is no network connection, false otherwise.
     */
    private boolean isOffline() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return !(manager != null
                && manager.getActiveNetworkInfo() != null
                && manager.getActiveNetworkInfo().isConnectedOrConnecting());
    }
}
