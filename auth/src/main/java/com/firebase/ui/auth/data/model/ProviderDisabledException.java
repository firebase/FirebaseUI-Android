package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;

/**
 * Represents an error in which the user has an account with a given provider, but the developer
 * disabled said provider. Usually occurs when linking accounts or retrieving credentials from Smart
 * Lock.
 */
public class ProviderDisabledException extends FirebaseUiException {
    /**
     * @param providerId the disabled provider that could no longer be used
     */
    public ProviderDisabledException(@NonNull String providerId) {
        super(ErrorCodes.DEVELOPER_ERROR, providerId + " provider not enabled");
    }
}
