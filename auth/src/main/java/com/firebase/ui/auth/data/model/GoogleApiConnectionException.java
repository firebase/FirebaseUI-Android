package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

import com.firebase.ui.auth.ErrorCodes;

/**
 * Represents an unrecoverable error connecting to Google Play Services.
 */
public class GoogleApiConnectionException extends FirebaseUiException {
    public GoogleApiConnectionException(@NonNull String detailMessage) {
        super(ErrorCodes.PLAY_SERVICES_ERROR, detailMessage);
    }
}
