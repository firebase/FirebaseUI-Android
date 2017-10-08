package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

public class UserCancellationException extends UnknownErrorException {
    public UserCancellationException(@NonNull String detailMessage) {
        super(detailMessage);
    }
}
