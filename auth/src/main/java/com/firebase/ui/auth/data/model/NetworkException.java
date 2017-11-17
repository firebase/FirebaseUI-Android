package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

import com.firebase.ui.auth.ErrorCodes;

/**
 * Represents an error connecting to the internet.
 */
public class NetworkException extends FirebaseUiException {
    public NetworkException(@NonNull String detailMessage) {
        super(ErrorCodes.NO_NETWORK, detailMessage);
    }
}
