package com.firebase.ui.auth.data.model;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

/**
 * Object representation of an {@link android.app.Activity#startIntentSenderForResult(IntentSender,
 * int, Intent, int, int, int) intent sender request}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
