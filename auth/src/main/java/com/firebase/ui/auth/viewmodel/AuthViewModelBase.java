package com.firebase.ui.auth.viewmodel;

import android.app.Application;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.content.Intent;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AuthViewModelBase extends ViewModelBase<FlowParameters> {

    private CredentialsClient mCredentialsClient;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider mPhoneAuth;

    private SingleLiveEvent<PendingResolution> mPendingResolution = new SingleLiveEvent<>();

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

    protected FirebaseAuth getAuth() {
        return mAuth;
    }

    protected PhoneAuthProvider getPhoneAuth() {
        return mPhoneAuth;
    }

    protected CredentialsClient getCredentialsClient() {
        return mCredentialsClient;
    }

    /**
     * Get an observable stream of {@link PendingIntent} resolutions requested by the ViewModel.
     *
     * Make sure to call {@link #onActivityResult(int, int, Intent)} for all activity results
     * after firing these pending intents.
     */
    public LiveData<PendingResolution> getPendingResolution() {
        return mPendingResolution;
    }

    protected void setPendingResolution(PendingResolution resolution) {
        mPendingResolution.setValue(resolution);
    }

    /**
     * Delegate activity result handling to the ViewModel. Returns {@code true} if the result was
     * handled.
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
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
