package com.firebase.ui.auth.password;

import android.content.Context;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.ui.auth.core.FirebaseAuthProvider;
import com.firebase.ui.auth.core.FirebaseResponse;
import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.AuthProviderType;
import com.firebase.ui.auth.core.FirebaseSignupError;
import com.firebase.ui.auth.core.TokenAuthHandler;

import java.util.Map;

public class PasswordAuthProvider extends FirebaseAuthProvider {

    private final String LOG_TAG = "PasswordAuthProvider";

    public PasswordAuthProvider(Context context, AuthProviderType providerType, String providerName, Firebase ref, TokenAuthHandler handler) {
        super(context, providerType, providerName, ref, handler);
    }

    public void signup(String email, String password, String password2) {
        getFirebaseRef().createUser(email, password, new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                getHandler().onSignupSuccess(result);
            }
            @Override
            public void onError(FirebaseError firebaseError) {
                //TODO: handle Signup error reasons
                getHandler().onSignupUserError(new FirebaseSignupError(FirebaseResponse.MISC_PROVIDER_ERROR, firebaseError.toString()));
            }
        });
    }

    public void login(String email, String password) {
        getFirebaseRef().authWithPassword(email, password, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                getHandler().onLoginSuccess(authData);
            }
            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                getHandler().onLoginUserError(new FirebaseLoginError(FirebaseResponse.MISC_PROVIDER_ERROR, firebaseError.toString()));
            }
        });
    }

    public void logout() {}
}