package com.firebase.ui.auth.util;

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;

/**
 * Factory for instances of authentication classes. Should eventually be replaced by dependency
 * injection.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AuthHelper {
    private final FlowParameters mFlowParams;

    public AuthHelper(FlowParameters params) {
        mFlowParams = params;
    }

    public FirebaseAuth getFirebaseAuth() {
        return FirebaseAuth.getInstance(FirebaseApp.getInstance(mFlowParams.appName));
    }

    public CredentialsApi getCredentialsApi() {
        return Auth.CredentialsApi;
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return getFirebaseAuth().getCurrentUser();
    }

    public PhoneAuthProvider getPhoneAuthProvider() {
        return PhoneAuthProvider.getInstance(getFirebaseAuth());
    }
}
