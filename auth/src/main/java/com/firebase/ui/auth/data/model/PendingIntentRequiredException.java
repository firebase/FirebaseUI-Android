package com.firebase.ui.auth.data.model;

import android.app.PendingIntent;
import android.content.IntentSender;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PendingIntentRequiredException extends FirebaseUiException {
    private final PendingIntent mPendingIntent;
    private final IntentSender mIntentSender;
    private final int mRequestCode;

    /**
     * Constructor for cases when a PendingIntent is available.
     *
     * @param pendingIntent The PendingIntent required to complete the operation.
     * @param requestCode   The associated request code.
     */
    public PendingIntentRequiredException(@NonNull PendingIntent pendingIntent, int requestCode) {
        super(ErrorCodes.UNKNOWN_ERROR);
        mPendingIntent = pendingIntent;
        mIntentSender = null;
        mRequestCode = requestCode;
    }

    /**
     * Constructor for cases when an IntentSender is available.
     *
     * @param intentSender The IntentSender required to complete the operation.
     * @param requestCode  The associated request code.
     */
    public PendingIntentRequiredException(@NonNull IntentSender intentSender, int requestCode) {
        super(ErrorCodes.UNKNOWN_ERROR);
        mIntentSender = intentSender;
        mPendingIntent = null;
        mRequestCode = requestCode;
    }

    /**
     * Returns the PendingIntent, if available.
     *
     * @return The PendingIntent or null if not available.
     */
    public PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    /**
     * Returns the IntentSender, if available.
     *
     * @return The IntentSender or null if not available.
     */
    public IntentSender getIntentSender() {
        return mIntentSender;
    }

    public int getRequestCode() {
        return mRequestCode;
    }
}