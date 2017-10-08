package com.firebase.ui.auth;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.GoogleApiConnectionException;
import com.firebase.ui.auth.data.model.NetworkException;
import com.firebase.ui.auth.data.remote.SignInKickstarter;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.remote.PlayServicesHelper;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class KickoffActivity extends HelperActivityBase {
    private static final String TAG = "KickoffActivity";
    private static final String IS_WAITING_FOR_PLAY_SERVICES = "is_waiting_for_play_services";
    private static final int RC_PLAY_SERVICES = 1;

    private SignInKickstarter mKickstarter;
    private boolean mIsWaitingForPlayServices = false;

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return HelperActivityBase.createBaseIntent(context, KickoffActivity.class, flowParams);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mKickstarter = ViewModelProviders.of(this).get(SignInKickstarter.class);
        mKickstarter.setSignInHandler(getSignInHandler());

        if (savedInstanceState == null || savedInstanceState.getBoolean(IS_WAITING_FOR_PLAY_SERVICES)) {
            init();
        }
    }

    private void init() {
        if (isOffline()) {
            Log.d(TAG, "No network connection");
            finish(RESULT_CANCELED, IdpResponse.getErrorIntent(
                    new NetworkException("No network on boot")));
            return;
        }

        boolean isPlayServicesAvailable = PlayServicesHelper.makePlayServicesAvailable(
                this,
                RC_PLAY_SERVICES,
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish(RESULT_CANCELED, IdpResponse.getErrorIntent(
                                new GoogleApiConnectionException(
                                        "User cancelled Play Services availability request on boot")));
                    }
                });

        if (isPlayServicesAvailable) {
            mKickstarter.start();
        } else {
            mIsWaitingForPlayServices = true;
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
            if (resultCode == RESULT_OK) {
                mKickstarter.start();
            } else {
                finish(RESULT_CANCELED, IdpResponse.getErrorIntent(new GoogleApiConnectionException(
                        "Couldn't make Play Services available on boot")));
            }
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
