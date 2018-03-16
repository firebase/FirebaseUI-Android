package com.firebase.ui.auth.viewmodel.idp;

import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProviderParamsBase {
    private final ProvidersHandler mHandler;

    protected ProviderParamsBase(ProvidersHandler handler) {
        mHandler = handler;
    }

    public ProvidersHandler getHandler() {
        return mHandler;
    }
}
