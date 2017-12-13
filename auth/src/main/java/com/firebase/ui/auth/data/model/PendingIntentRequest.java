package com.firebase.ui.auth.data.model;

import android.app.PendingIntent;
import android.support.annotation.NonNull;

public final class PendingIntentRequest {
    private final PendingIntent mIntent;
    private final int mRequestCode;

    public PendingIntentRequest(@NonNull PendingIntent intent, int requestCode) {
        mIntent = intent;
        mRequestCode = requestCode;
    }

    @NonNull
    public PendingIntent getIntent() {
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
