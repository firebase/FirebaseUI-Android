package com.firebase.ui.auth.data.model;

import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class UserCollisionException extends FirebaseUiException {
    public UserCollisionException() {
        super(ErrorCodes.UNKNOWN_ERROR);
    }
}
