package com.firebase.ui.auth;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.GoogleApiConnectionException;
import com.firebase.ui.auth.data.model.NetworkException;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.data.remote.SignInKickstarter;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.PlayServicesHelper;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class KickoffActivity extends HelperActivityBase {
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
        mKickstarter.init(getFlowHolder());
        mKickstarter.setSignInHandler(getSignInHandler());
        getSignInHandler().getSignInLiveData().observe(this, new Observer<IdpResponse>() {
            @Override
            public void onChanged(@Nullable IdpResponse response) {
                finish(response.isSuccessful() ? Activity.RESULT_OK : Activity.RESULT_CANCELED,
                       response.toIntent());
            }
        });

        if (savedInstanceState == null || savedInstanceState.getBoolean(IS_WAITING_FOR_PLAY_SERVICES)) {
            init();
        }
    }

    private void init() {
        if (isOffline()) {
            finish(RESULT_CANCELED,
                   IdpResponse.fromError(new NetworkException("No network on boot")).toIntent());
            return;
        }

        boolean isPlayServicesAvailable = PlayServicesHelper.makePlayServicesAvailable(
                this,
                RC_PLAY_SERVICES,
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish(RESULT_CANCELED, IdpResponse.fromError(
                                new GoogleApiConnectionException(
                                        "User cancelled Play Services availability request on boot"))
                                .toIntent());
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
                finish(RESULT_CANCELED, IdpResponse.fromError(new GoogleApiConnectionException(
                        "Couldn't make Play Services available on boot")).toIntent());
            }
        }
    }

    @Override
    public void finish(int resultCode, Intent intent) {
        @NonNull IdpResponse response = IdpResponse.fromResultIntent(intent);
        // TODO return the full response when we decide to break backwards compatibility.
        // For now, if the user cancelled the request, we return a null response.

        super.finish(resultCode,
                response.isSuccessful() || !(response.getException() instanceof UserCancellationException) ?
                        intent : null);
    }

    /**
     * Check if there is an active or soon-to-be-active network connection.
     *
     * @return true if there is no network connection, false otherwise.
     */
    private boolean isOffline() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        return !(manager != null
                && manager.getActiveNetworkInfo() != null
                && manager.getActiveNetworkInfo().isConnectedOrConnecting());
    }
}
