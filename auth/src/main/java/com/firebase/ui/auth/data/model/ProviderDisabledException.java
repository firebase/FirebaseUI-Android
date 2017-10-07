package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

public class ProviderDisabledException extends UnknownErrorException {
    public ProviderDisabledException(@NonNull String providerId) {
        super(providerId + " provider not enabled");
    }
}
