package com.firebase.ui.auth;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.data.remote.SignInKickstarter;
import com.firebase.ui.auth.ui.InvisibleActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class KickoffActivity extends InvisibleActivityBase {
    private SignInKickstarter mKickstarter;

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return createBaseIntent(context, KickoffActivity.class, flowParams);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mKickstarter = ViewModelProviders.of(this).get(SignInKickstarter.class);
        mKickstarter.init(getFlowParams());
        mKickstarter.getOperation().observe(this, new ResourceObserver<IdpResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdpResponse response) {
                finish(RESULT_OK, response.toIntent());
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                if (e instanceof UserCancellationException) {
                    finish(RESULT_CANCELED, null);
                } else if (e instanceof FirebaseAuthAnonymousUpgradeException) {
                    IdpResponse res = ((FirebaseAuthAnonymousUpgradeException) e).getResponse();
                    finish(RESULT_CANCELED, new Intent().putExtra(ExtraConstants.IDP_RESPONSE, res));
                } else {
                    finish(RESULT_CANCELED, IdpResponse.getErrorIntent(e));
                }
            }
        });

        GoogleApiAvailability.getInstance()
                .makeGooglePlayServicesAvailable(this)
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if (savedInstanceState != null) { return; }

                        if (isOffline()) {
                            finish(RESULT_CANCELED, IdpResponse.getErrorIntent(
                                    new FirebaseUiException(ErrorCodes.NO_NETWORK)));
                        } else {
                            mKickstarter.start();
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        finish(RESULT_CANCELED, IdpResponse.getErrorIntent(new FirebaseUiException(
                                ErrorCodes.PLAY_SERVICES_UPDATE_CANCELLED, e)));
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mKickstarter.onActivityResult(requestCode, resultCode, data);
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
