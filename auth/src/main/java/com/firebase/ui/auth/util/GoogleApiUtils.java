package com.firebase.ui.auth.util;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class GoogleApiUtils {
    private GoogleApiUtils() {
        throw new AssertionError("No instance for you!");
    }

    @NonNull
    public static CredentialsClient getCredentialsClient(@NonNull Context context) {
        CredentialsClient client;
        if (context instanceof Activity) {
            client = Credentials.getClient((Activity) context);
        } else {
            client = Credentials.getClient(context);
        }
        return client;
    }
}
