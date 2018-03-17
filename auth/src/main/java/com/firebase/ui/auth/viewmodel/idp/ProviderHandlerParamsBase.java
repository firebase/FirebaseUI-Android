package com.firebase.ui.auth.viewmodel.idp;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProviderHandlerParamsBase {
    private final ProvidersHandlerBase mHandler;

    protected ProviderHandlerParamsBase(@NonNull ProvidersHandlerBase handler) {
        mHandler = handler;
    }

    @NonNull
    public ProvidersHandlerBase getHandler() {
        return mHandler;
    }
}
