package com.firebase.ui.auth.data.model;

/**
 * Base class for all FirebaseUI exceptions.
 */
public abstract class FirebaseUiException extends Exception {
    public FirebaseUiException(String message) {
        super(message);
    }

    public FirebaseUiException(String message, Throwable cause) {
        super(message, cause);
    }

    public FirebaseUiException(Throwable cause) {
        super(cause);
    }
}
