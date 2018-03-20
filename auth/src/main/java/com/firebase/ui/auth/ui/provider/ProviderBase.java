package com.firebase.ui.auth.ui.provider;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.viewmodel.idp.ProviderResponseHandlerBase;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProviderBase implements Provider {
    private final ProviderResponseHandlerBase mHandler;

    public ProviderBase(@NonNull ProviderResponseHandlerBase handler) {
        mHandler = handler;
    }

    @NonNull
    protected ProviderResponseHandlerBase getProvidersHandler() {
        return mHandler;
    }
}
