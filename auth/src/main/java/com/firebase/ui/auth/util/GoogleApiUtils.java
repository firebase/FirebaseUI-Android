package com.firebase.ui.auth.util;

import android.content.Context;

import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.credentials.CredentialManager;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class GoogleApiUtils {
    private GoogleApiUtils() {
        throw new AssertionError("No instance for you!");
    }

    public static boolean isPlayServicesAvailable(@NonNull Context context) {
        return GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }

    @NonNull
    public static SignInClient getSignInClient(@NonNull Context context) {
        return Identity.getSignInClient(context);
    }

    @NonNull
    public static CredentialManager getCredentialManager(@NonNull Context context) {
        return CredentialManager.create(context);
    }
}
