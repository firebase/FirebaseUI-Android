package com.firebase.ui.auth.data.model;

/**
 * Base class for all FirebaseUI exceptions.
 */
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
