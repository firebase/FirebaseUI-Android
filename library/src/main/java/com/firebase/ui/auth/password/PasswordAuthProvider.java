package com.firebase.ui.auth.password;

import android.app.Activity;
import android.content.Context;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.ui.auth.core.FirebaseAuthProvider;
import com.firebase.ui.auth.core.FirebaseResponse;
import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.SocialProvider;
import com.firebase.ui.auth.core.TokenAuthHandler;

public class PasswordAuthProvider extends FirebaseAuthProvider {

    private final String LOG_TAG = "PasswordAuthProvider";

    public final static String PROVIDER_NAME = "password";
    public final static SocialProvider PROVIDER_TYPE = SocialProvider.password;

    private TokenAuthHandler mHandler;
    private Activity mActivity;
    private Firebase mRef;

    public PasswordAuthProvider(Context context, Firebase ref, TokenAuthHandler handler) {
        mActivity = (Activity) context;
        mRef = ref;
        mHandler = handler;
    }

    public void login(String email, String password) {
        mRef.authWithPassword(email, password, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                mHandler.onSuccess(authData);
            }
            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                mHandler.onUserError(new FirebaseLoginError(FirebaseResponse.MISC_PROVIDER_ERROR, firebaseError.toString()));
            }
        });
    }

    public String getProviderName() { return PROVIDER_NAME; }
    public Firebase getFirebaseRef() { return mRef; }
    public SocialProvider getProviderType() { return PROVIDER_TYPE; };
    public void logout() {}
}
