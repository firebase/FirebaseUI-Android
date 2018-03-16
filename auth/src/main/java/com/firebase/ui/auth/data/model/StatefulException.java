package com.firebase.ui.auth.data.model;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;

public class StatefulException extends FirebaseUiException {
    public StatefulException() {
        super(ErrorCodes.UNKNOWN_ERROR);
    }
}
