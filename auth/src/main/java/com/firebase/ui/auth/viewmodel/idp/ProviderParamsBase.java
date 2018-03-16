package com.firebase.ui.auth.viewmodel.idp;

import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProviderParamsBase {
    private final ProvidersHandlerBase mHandler;

    protected ProviderParamsBase(ProvidersHandlerBase handler) {
        mHandler = handler;
    }

    public ProvidersHandlerBase getHandler() {
        return mHandler;
    }
}
