package com.firebase.ui.auth.data.model;

import android.app.PendingIntent;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PendingIntentRequiredException extends StatefulException {
    private final PendingIntent mPendingIntent;
    private final int mRequestCode;

    public PendingIntentRequiredException(@NonNull PendingIntent pendingIntent, int requestCode) {
        mPendingIntent = pendingIntent;
        mRequestCode = requestCode;
    }

    @NonNull
    public PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    public int getRequestCode() {
        return mRequestCode;
    }
}
