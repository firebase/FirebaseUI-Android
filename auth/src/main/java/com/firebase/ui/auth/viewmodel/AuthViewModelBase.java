package com.firebase.ui.auth.viewmodel;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
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
public abstract class AuthViewModelBase<T> extends ViewModelBase<FlowParameters> {
    private CredentialsClient mCredentialsClient;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider mPhoneAuth;

    private MutableLiveData<Resource<T>> mOperation = new MutableLiveData<>();

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

    /**
     * Get the observable state of the sign in operation.
     */
    public LiveData<Resource<T>> getOperation() {
        return mOperation;
    }

    /**
     * Delegate activity result handling to the ViewModel. Returns {@code true} if the result was
     * handled.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {}

    protected void setResult(Resource<T> result) {
        mOperation.setValue(result);
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
