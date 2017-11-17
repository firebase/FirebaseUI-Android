package com.firebase.ui.auth.provider;

import com.firebase.ui.auth.IdpResponse;

public interface IdpProvider extends Provider {
    interface IdpCallback {
        void onSuccess(IdpResponse idpResponse);

        void onFailure();
    }

    void setAuthenticationCallback(IdpCallback callback);
}
