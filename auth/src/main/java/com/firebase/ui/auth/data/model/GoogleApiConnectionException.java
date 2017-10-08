package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

public class GoogleApiConnectionException extends UnknownErrorException {
    public GoogleApiConnectionException(@NonNull String detailMessage) {
        super(detailMessage);
    }
}
