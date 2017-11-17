package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

import com.firebase.ui.auth.ErrorCodes;

/**
 * Represents a user cancellation, usually through the Android back button.
 */
public class UserCancellationException extends FirebaseUiException {
    public UserCancellationException(@NonNull String detailMessage) {
        super(ErrorCodes.CANCELLED, detailMessage);
    }
}
