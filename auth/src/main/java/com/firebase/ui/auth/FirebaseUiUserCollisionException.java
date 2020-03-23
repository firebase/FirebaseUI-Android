package com.firebase.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.firebase.auth.AuthCredential;

/**
 * Internal exception which holds the necessary data to complete sign-in in the event of a
 * recoverable error.
 */
public class FirebaseUiUserCollisionException extends Exception {

    private final int mErrorCode;
    private final String mProviderId;
    private final String mEmail;
    private final AuthCredential mCredential;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiUserCollisionException(@ErrorCodes.Code int code,
                                            @NonNull String message,
                                            @NonNull String providerId,
                                            @NonNull String email,
                                            @NonNull AuthCredential credential) {
        super(message);
        mErrorCode = code;
        mProviderId = providerId;
        mEmail = email;
        mCredential = credential;
    }

    @NonNull
    public String getProviderId() {
        return mProviderId;
    }

    @NonNull
    public String getEmail() {
        return mEmail;
    }

    @NonNull
    public AuthCredential getCredential() {
        return mCredential;
    }

    /**
     * @return error code associated with this exception
     * @see ErrorCodes
     */
    @ErrorCodes.Code
    public final int getErrorCode() {
        return mErrorCode;
    }
}
