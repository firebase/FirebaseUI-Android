package com.firebase.ui.auth.data.remote;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.viewmodel.idp.ProviderHandler;
import com.firebase.ui.auth.viewmodel.idp.ProvidersHandler;

public final class FacebookParams extends ProviderHandler.ParamsBase {
    private final AuthUI.IdpConfig mConfig;

    public FacebookParams(ProvidersHandler handler, AuthUI.IdpConfig config) {
        super(handler);
        mConfig = config;
    }

    public AuthUI.IdpConfig getConfig() {
        return mConfig;
    }
}
