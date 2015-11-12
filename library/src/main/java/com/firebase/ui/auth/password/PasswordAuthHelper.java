package com.firebase.ui.auth.password;

import android.app.Activity;
import android.content.Context;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.ui.auth.core.FirebaseAuthHelper;
import com.firebase.ui.auth.core.TokenAuthHandler;

public class PasswordAuthHelper extends FirebaseAuthHelper {

    private final String LOG_TAG = "PasswordAuthHelper";

    public final static String PROVIDER_NAME = "password";

    private TokenAuthHandler mHandler;
    private Activity mActivity;
    private Firebase mRef;

    public PasswordAuthHelper(Context context, Firebase ref, TokenAuthHandler handler) {
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
                mHandler.onUserError(firebaseError);
            }
        });
    }

    public String getProviderName() { return PROVIDER_NAME; }
    public Firebase getFirebaseRef() { return mRef; }
    public void logout() {}
}
