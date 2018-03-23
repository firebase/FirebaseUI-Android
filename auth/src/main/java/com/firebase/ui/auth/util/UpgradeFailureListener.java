package com.firebase.ui.auth.util;

import android.support.annotation.NonNull;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

/**
 * Failure listener for use with AnonymousUpgradeUtils.
 */
public abstract class UpgradeFailureListener implements OnFailureListener {

    private final FlowParameters mParameters;
    private final FirebaseAuth mAuth;
    private final AuthCredential mCredential;

    public UpgradeFailureListener(FlowParameters parameters,
                                  FirebaseAuth auth,
                                  AuthCredential credential) {
        mParameters = parameters;
        mAuth = auth;
        mCredential = credential;
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        if (AnonymousUpgradeUtils.isUpgradeFailure(mParameters, mAuth, e)) {
            onUpgradeFailure((FirebaseAuthUserCollisionException) e);
        } else {
            onNonUpgradeFailure(e);
        }
    }

    protected void onUpgradeFailure(@NonNull FirebaseAuthUserCollisionException e) {
        // In the case of Phone Auth the exception contains an updated credential we can
        // user since the other credential is expired after one use.
        AuthCredential credential = e.getUpdatedCredential() != null
                ? e.getUpdatedCredential()
                : mCredential;

        IdpResponse response = new IdpResponse.Builder(credential).build();
        onUpgradeFailure(response);
    }

    protected void onUpgradeFailure(@NonNull IdpResponse response) {}

    protected abstract void onNonUpgradeFailure(@NonNull Exception e);
}
