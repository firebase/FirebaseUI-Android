package com.firebase.ui.auth.data.remote;

import android.support.annotation.Nullable;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.viewmodel.idp.ProviderHandler;
import com.firebase.ui.auth.viewmodel.idp.ProvidersHandler;

public final class GoogleParams extends ProviderHandler.ParamsBase {
    private final AuthUI.IdpConfig mConfig;
    @Nullable private final String mEmail;

    public GoogleParams(ProvidersHandler handler, AuthUI.IdpConfig config, @Nullable String email) {
        super(handler);
        mConfig = config;
        mEmail = email;
    }

    public AuthUI.IdpConfig getConfig() {
        return mConfig;
    }

    @Nullable
    public String getEmail() {
        return mEmail;
    }
}
