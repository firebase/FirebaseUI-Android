package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

import com.firebase.ui.auth.ErrorCodes;
import com.google.firebase.auth.FirebaseAuthException;

public class NetworkException extends FirebaseAuthException {
    public NetworkException(@NonNull String detailMessage) {
        super(String.valueOf(ErrorCodes.NO_NETWORK), detailMessage);
    }
}
