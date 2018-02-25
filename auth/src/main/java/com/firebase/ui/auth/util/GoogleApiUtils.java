package com.firebase.ui.auth.util;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.credentials.CredentialsOptions;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class GoogleApiUtils {
    private GoogleApiUtils() {
        throw new AssertionError("No instance for you!");
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
