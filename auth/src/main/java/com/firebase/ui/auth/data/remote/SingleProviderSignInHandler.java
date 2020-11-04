package com.firebase.ui.auth.data.remote;

import android.app.Application;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class SingleProviderSignInHandler<T> extends ProviderSignInBase<T> {

    private final String mProviderId;

    protected SingleProviderSignInHandler(Application application, String providerId) {
        super(application);
        this.mProviderId = providerId;
    }

    @Override
    public void startSignIn(@NonNull HelperActivityBase activity) {
        super.startSignIn(activity);
        this.startSignIn(activity.getAuth(), activity, mProviderId);
    }
}
