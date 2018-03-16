package com.firebase.ui.auth.ui.provider;

import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.viewmodel.idp.ProvidersHandler;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProviderBase implements Provider {
    private final ProvidersHandler mHandler;

    public ProviderBase(ProvidersHandler handler) {
        mHandler = handler;
    }

    protected ProvidersHandler getProvidersHandler() {
        return mHandler;
    }
}
