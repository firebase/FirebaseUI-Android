package com.firebase.ui.auth.util;

import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;

/**
 * Factory for instances of authentication classes. Should eventually be replaced by
 * dependency injection.
 */
public class AuthInstances {

    public static FirebaseAuth getFirebaseAuth(FlowParameters parameters) {
        return FirebaseAuth.getInstance(FirebaseApp.getInstance(parameters.appName));
    }

    public static CredentialsApi getCredentialsApi() {
        return Auth.CredentialsApi;
    }

    @Nullable
    public static FirebaseUser getCurrentUser(FlowParameters parameters) {
        return getFirebaseAuth(parameters).getCurrentUser();
    }

    public static SaveSmartLock getSaveSmartLockInstance(FragmentActivity activity,
                                                         FlowParameters parameters) {
        return SaveSmartLock.getInstance(activity, parameters);
    }

    public static PhoneAuthProvider getPhoneAuthProviderInstance() {
        return PhoneAuthProvider.getInstance();
    }


}
