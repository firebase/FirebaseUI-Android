package com.firebase.ui.auth.data.remote;

import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class GitHubSignInHandlerBridge {
    public static final Class<ProviderSignInBase<AuthUI.IdpConfig>> HANDLER_CLASS;

    static {
        try {
            //noinspection unchecked
            HANDLER_CLASS = (Class<ProviderSignInBase<AuthUI.IdpConfig>>)
                    Class.forName("com.firebase.ui.auth.data.remote.GitHubSignInHandler");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Check for availability with ProviderAvailability first.", e);
        }
    }

    private GitHubSignInHandlerBridge() {
        throw new AssertionError("No instance for you!");
    }
}
