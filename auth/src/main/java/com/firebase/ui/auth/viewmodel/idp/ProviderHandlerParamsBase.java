package com.firebase.ui.auth.viewmodel.idp;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProviderHandlerParamsBase {
    private final ProviderResponseHandlerBase mHandler;

    protected ProviderHandlerParamsBase(@NonNull ProviderResponseHandlerBase handler) {
        mHandler = handler;
    }

    @NonNull
    public ProviderResponseHandlerBase getHandler() {
        return mHandler;
    }
}
