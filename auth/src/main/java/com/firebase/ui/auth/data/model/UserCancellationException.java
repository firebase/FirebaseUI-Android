package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

/**
 * Represents a user cancellation, usually through the Android back button.
 */
public class UserCancellationException extends UnknownErrorException {
    public UserCancellationException(@NonNull String detailMessage) {
        super(detailMessage);
    }
}
