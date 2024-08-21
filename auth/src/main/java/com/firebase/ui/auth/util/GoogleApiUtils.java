package com.firebase.ui.auth.util;

import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class GoogleApiUtils {
    private GoogleApiUtils() {
        throw new AssertionError("No instance for you!");
    }

    public static boolean isPlayServicesAvailable(@NonNull Context context) {
        return GoogleApiAvailability
                .getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }
}
