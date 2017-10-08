package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

public class ProviderErrorException extends UnknownErrorException {
    public ProviderErrorException(@NonNull String detailMessage) {
        super(detailMessage);
    }
}
