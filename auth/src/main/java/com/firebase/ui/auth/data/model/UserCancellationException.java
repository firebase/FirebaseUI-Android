package com.firebase.ui.auth.data.model;

import com.firebase.ui.auth.ErrorCodes;

/**
 * Represents a user cancellation, usually through the Android back button.
 */
public class UserCancellationException extends FirebaseUiException {
    public UserCancellationException() {
        super(ErrorCodes.UNKNOWN_ERROR);
    }
}
