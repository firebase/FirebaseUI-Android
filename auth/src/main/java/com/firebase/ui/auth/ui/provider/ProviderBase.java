package com.firebase.ui.auth.ui.provider;

import com.firebase.ui.auth.viewmodel.idp.ProvidersHandler;

public abstract class ProviderBase implements Provider {
    private final ProvidersHandler mHandler;

    public ProviderBase(ProvidersHandler handler) {
        mHandler = handler;
    }

    protected ProvidersHandler getProvidersHandler() {
        return mHandler;
    }
}
