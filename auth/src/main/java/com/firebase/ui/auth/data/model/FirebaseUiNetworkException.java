package com.firebase.ui.auth.data.model;

/**
 * Represents an error connecting to the internet.
 */
public class FirebaseUiNetworkException extends FirebaseUiException {
    public FirebaseUiNetworkException(String message) {
        super(message);
    }
}
