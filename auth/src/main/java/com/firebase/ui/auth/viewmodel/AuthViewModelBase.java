package com.firebase.ui.auth.viewmodel;

import android.app.Application;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class AuthViewModelBase<T> extends OperableViewModel<FlowParameters, Resource<T>> {
    private SignInClient mSignInClient;
    private FirebaseAuth mAuth;

    protected AuthViewModelBase(Application application) {
        super(application);
    }

    @Override
    protected void onCreate() {
        FirebaseApp app = FirebaseApp.getInstance(getArguments().appName);
        mAuth = FirebaseAuth.getInstance(app);
        mSignInClient = GoogleApiUtils.getSignInClient(getApplication());
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    protected FirebaseAuth getAuth() {
        return mAuth;
    }

    protected SignInClient getSignInClient() {
        return mSignInClient;
    }

    @VisibleForTesting
    public void initializeForTesting(FlowParameters parameters,
                                     FirebaseAuth auth,
                                     SignInClient client) {
        setArguments(parameters);
        mAuth = auth;
        mSignInClient = client;
    }
}