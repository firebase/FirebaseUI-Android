package com.firebase.ui.auth.viewmodel;

import android.app.Application;
import android.support.annotation.VisibleForTesting;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthProvider;

public class AuthViewModelBase extends ViewModelBase<FlowParameters> {

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
        mCredentialsClient = Credentials.getClient(getApplication());
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
