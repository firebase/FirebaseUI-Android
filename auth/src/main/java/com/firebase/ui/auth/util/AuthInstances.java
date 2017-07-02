package com.firebase.ui.auth.util;

import android.support.annotation.Nullable;

import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
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
public class AuthInstances {
    private static FlowParameters sFlowParams;

    public static void init(FlowParameters params) {
        sFlowParams = params;
    }

    public static FirebaseAuth getFirebaseAuth() {
        return FirebaseAuth.getInstance(FirebaseApp.getInstance(sFlowParams.appName));
    }

    public static CredentialsApi getCredentialsApi() {
        return Auth.CredentialsApi;
    }

    @Nullable
    public static FirebaseUser getCurrentUser() {
        return getFirebaseAuth().getCurrentUser();
    }

    public static SaveSmartLock getSaveSmartLockInstance(HelperActivityBase activity) {
        return SaveSmartLock.getInstance(activity);
    }

    public static PhoneAuthProvider getPhoneAuthProviderInstance() {
        return PhoneAuthProvider.getInstance();
    }


}
