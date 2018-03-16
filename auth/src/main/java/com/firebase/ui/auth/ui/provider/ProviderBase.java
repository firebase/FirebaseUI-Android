package com.firebase.ui.auth.ui.provider;

import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.viewmodel.idp.ProvidersHandlerBase;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProviderBase implements Provider {
    private final ProvidersHandlerBase mHandler;

    public ProviderBase(ProvidersHandlerBase handler) {
        mHandler = handler;
    }

    protected ProvidersHandlerBase getProvidersHandler() {
        return mHandler;
    }
}
