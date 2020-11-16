package com.firebase.ui.auth.util;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.credentials.CredentialsOptions;
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

    @NonNull
    public static CredentialsClient getCredentialsClient(@NonNull Context context) {
        CredentialsOptions options = new CredentialsOptions.Builder()
                .forceEnableSaveDialog()
                .build();
        if (context instanceof Activity) {
            return Credentials.getClient((Activity) context, options);
        } else {
            return Credentials.getClient(context, options);
        }
    }
}
