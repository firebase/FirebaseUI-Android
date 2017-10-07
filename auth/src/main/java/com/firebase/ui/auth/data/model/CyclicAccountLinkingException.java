package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

public class CyclicAccountLinkingException extends UnknownErrorException {
    public CyclicAccountLinkingException(@NonNull String detailMessage) {
        super(detailMessage);
    }
}
