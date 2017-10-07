package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

import com.firebase.ui.auth.ErrorCodes;
import com.google.firebase.auth.FirebaseAuthException;

public class UnknownErrorException extends FirebaseAuthException {
    public UnknownErrorException(@NonNull String detailMessage) {
        super(String.valueOf(ErrorCodes.UNKNOWN_ERROR), detailMessage);
    }
}
