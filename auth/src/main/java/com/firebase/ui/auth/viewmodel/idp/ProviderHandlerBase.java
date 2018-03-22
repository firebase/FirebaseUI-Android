package com.firebase.ui.auth.viewmodel.idp;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.viewmodel.OperableViewModel;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProviderHandlerBase<T> extends OperableViewModel<T, IdpResponse> {
    public ProviderHandlerBase(Application application) {
        super(application);
    }

    public abstract void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);
}
