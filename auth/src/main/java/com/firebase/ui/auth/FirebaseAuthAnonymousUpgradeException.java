package com.firebase.ui.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FirebaseAuthAnonymousUpgradeException extends Exception {

    private IdpResponse response;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseAuthAnonymousUpgradeException(@ErrorCodes.Code int code,
                                                 @NonNull IdpResponse response) {
        super(ErrorCodes.toFriendlyMessage(code));
        this.response = response;
    }

    public IdpResponse getResponse() {
        return response;
    }
}
