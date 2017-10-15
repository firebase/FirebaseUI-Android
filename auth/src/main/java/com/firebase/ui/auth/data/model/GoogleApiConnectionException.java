package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

/**
 * Represents an unrecoverable error connecting to Google Play Services.
 */
public class GoogleApiConnectionException extends UnknownErrorException {
    public GoogleApiConnectionException(@NonNull String detailMessage) {
        super(detailMessage);
    }
}
