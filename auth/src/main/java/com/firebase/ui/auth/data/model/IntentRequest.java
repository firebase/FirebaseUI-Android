package com.firebase.ui.auth.data.model;

import android.content.Intent;
import android.support.annotation.NonNull;

public final class IntentRequest {
    private final Intent mIntent;
    private final int mRequestCode;

    public IntentRequest(@NonNull Intent intent, int requestCode) {
        mIntent = intent;
        mRequestCode = requestCode;
    }

    @NonNull
    public Intent getIntent() {
        return mIntent;
    }

    public int getRequestCode() {
        return mRequestCode;
    }

    @Override
    public String toString() {
        return "IntentRequest{" +
                "mIntent=" + mIntent +
                ", mRequestCode=" + mRequestCode +
                '}';
    }
}
