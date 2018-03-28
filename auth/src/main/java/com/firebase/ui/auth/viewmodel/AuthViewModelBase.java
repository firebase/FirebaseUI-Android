package com.firebase.ui.auth.viewmodel;

import android.app.Application;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class AuthViewModelBase<T> extends OperableViewModel<FlowParameters, Resource<T>> {
    private CredentialsClient mCredentialsClient;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider mPhoneAuth;

    protected AuthViewModelBase(Application application) {
        super(application);
    }

    @Override
    protected void onCreate() {
        FirebaseApp app = FirebaseApp.getInstance(getArguments().appName);
        mAuth = FirebaseAuth.getInstance(app);
        mPhoneAuth = PhoneAuthProvider.getInstance(mAuth);
        mCredentialsClient = GoogleApiUtils.getCredentialsClient(getApplication());
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    protected FirebaseAuth getAuth() {
        return mAuth;
    }

    protected PhoneAuthProvider getPhoneAuth() {
        return mPhoneAuth;
    }

    protected CredentialsClient getCredentialsClient() {
        return mCredentialsClient;
    }

    @VisibleForTesting
    public void initializeForTesting(FlowParameters parameters,
                                     FirebaseAuth auth,
                                     CredentialsClient client,
                                     PhoneAuthProvider phoneAuth) {
        setArguments(parameters);
        mAuth = auth;
        mCredentialsClient = client;
        mPhoneAuth = phoneAuth;
    }
}
