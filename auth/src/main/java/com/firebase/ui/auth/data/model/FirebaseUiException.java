package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.ErrorCodes;

/**
 * Base class for all FirebaseUI exceptions.
 */
public class FirebaseUiException extends Exception {
    private final int mErrorCode;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiException(@ErrorCodes.All int code) {
        mErrorCode = code;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiException(@ErrorCodes.All int code, @NonNull String message) {
        super(message);
        mErrorCode = code;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiException(@ErrorCodes.All int code,
                               @NonNull String message,
                               @NonNull Throwable cause) {
        super(message, cause);
        mErrorCode = code;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiException(@ErrorCodes.All int code, @NonNull Throwable cause) {
        super(cause);
        mErrorCode = code;
    }

    /**
     * @return error code associated with this exception
     * @see com.firebase.ui.auth.ErrorCodes
     */
    @ErrorCodes.All
    public final int getErrorCode() {
        return mErrorCode;
    }
}
