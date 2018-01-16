package com.firebase.ui.auth.viewmodel;

import android.app.PendingIntent;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

/**
 * POJO holding the data for a resolution that some Activity needs to fire.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PendingResolution {

    private PendingIntent mPendingIntent;
    private int mRequestCode;

    public PendingResolution(@NonNull PendingIntent pendingIntent, int requestCode) {
        this.mPendingIntent = pendingIntent;
        this.mRequestCode = requestCode;
    }

    @NonNull
    public PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    public int getRequestCode() {
        return mRequestCode;
    }

}
