package com.firebase.ui.auth.data.model;

/**
 * @deprecated migrate internals to use use exceptions which will be known
 */
@Deprecated
public class FirebaseUiUnknownErrorException extends FirebaseUiException {
    public FirebaseUiUnknownErrorException(String message) {
        super(message);
    }
}
