package com.firebase.ui.auth.util;

import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuthException;

public class SignInFailedException extends FirebaseAuthException {
    public SignInFailedException(@NonNull String errorCode, @NonNull String detailMessage) {
        super(errorCode, detailMessage);
    }
}
