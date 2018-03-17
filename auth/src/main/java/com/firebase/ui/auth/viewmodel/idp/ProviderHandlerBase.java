package com.firebase.ui.auth.viewmodel.idp;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.viewmodel.ViewModelBase;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProviderHandlerBase<T extends ProviderHandlerParamsBase> extends ViewModelBase<T> {
    public ProviderHandlerBase(Application application) {
        super(application);
    }

    protected void setResult(@NonNull IdpResponse result) {
        getArguments().getHandler().startSignIn(result);
    }

    public abstract void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);
}
