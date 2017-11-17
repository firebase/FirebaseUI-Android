package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

import com.firebase.ui.auth.ErrorCodes;

/**
 * Represents some error using a provider.
 */
public class ProviderErrorException extends FirebaseUiException {
    public ProviderErrorException(@NonNull String detailMessage) {
        super(ErrorCodes.PROVIDER_ERROR, detailMessage);
    }
}
