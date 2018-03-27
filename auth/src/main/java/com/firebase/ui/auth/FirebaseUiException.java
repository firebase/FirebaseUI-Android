package com.firebase.ui.auth;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

/**
 * Base class for all FirebaseUI exceptions.
 */
public class FirebaseUiException extends Exception {
    private final int mErrorCode;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiException(@ErrorCodes.Code int code) {
        this(code, ErrorCodes.toFriendlyMessage(code));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiException(@ErrorCodes.Code int code, @NonNull String message) {
        super(message);
        mErrorCode = code;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiException(@ErrorCodes.Code int code, @NonNull Throwable cause) {
        this(code, ErrorCodes.toFriendlyMessage(code), cause);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiException(@ErrorCodes.Code int code,
                               @NonNull String message,
                               @NonNull Throwable cause) {
        super(message, cause);
        mErrorCode = code;
    }

    /**
     * @return error code associated with this exception
     * @see com.firebase.ui.auth.ErrorCodes
     */
    @ErrorCodes.Code
    public final int getErrorCode() {
        return mErrorCode;
    }
}
