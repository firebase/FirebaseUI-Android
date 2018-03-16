package com.firebase.ui.auth.data.model;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;

public class StatefulException extends FirebaseUiException {
    private boolean mUsed;

    public StatefulException() {
        super(ErrorCodes.UNKNOWN_ERROR);
    }

    public boolean isUsed() {
        return mUsed;
    }

    public void setUsed(boolean used) {
        mUsed = used;
    }
}
