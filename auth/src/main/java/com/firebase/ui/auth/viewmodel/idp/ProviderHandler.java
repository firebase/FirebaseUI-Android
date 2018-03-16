package com.firebase.ui.auth.viewmodel.idp;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.viewmodel.ViewModelBase;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProviderHandler<T extends ProviderHandler.ParamsBase> extends ViewModelBase<T> {
    public ProviderHandler(Application application) {
        super(application);
    }

    protected void setResult(IdpResponse result) {
        getArguments().getHandler().startSignIn(result);
    }

    public abstract void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);

    public abstract static class ParamsBase {
        private final ProvidersHandler mHandler;

        protected ParamsBase(ProvidersHandler handler) {
            mHandler = handler;
        }

        public ProvidersHandler getHandler() {
            return mHandler;
        }
    }
}
