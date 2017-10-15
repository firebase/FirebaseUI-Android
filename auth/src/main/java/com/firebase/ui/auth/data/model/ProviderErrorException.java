package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

/**
 * Represents some error using a provider.
 */
public class ProviderErrorException extends UnknownErrorException {
    public ProviderErrorException(@NonNull String detailMessage) {
        super(detailMessage);
    }
}
