package com.firebase.ui.auth.data.model;

import android.support.annotation.RestrictTo;

/**
 * Base class for all FirebaseUI exceptions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FirebaseUiException extends Exception {
    private final int mErrorCode;

    public FirebaseUiException(int code) {
        mErrorCode = code;
    }

    public FirebaseUiException(int code, String message) {
        super(message);
        mErrorCode = code;
    }

    public FirebaseUiException(int code, String message, Throwable cause) {
        super(message, cause);
        mErrorCode = code;
    }

    public FirebaseUiException(int code, Throwable cause) {
        super(cause);
        mErrorCode = code;
    }

    public final int getErrorCode() {
        return mErrorCode;
    }
}
