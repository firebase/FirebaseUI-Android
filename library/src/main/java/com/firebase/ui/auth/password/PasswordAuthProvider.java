package com.firebase.ui.auth.password;

import android.content.Context;
import android.util.Log;

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
    private String myEmail, myPassword;

    public PasswordAuthProvider(Context context, AuthProviderType providerType, String providerName, Firebase ref, TokenAuthHandler handler) {
        super(context, providerType, providerName, ref, handler);
    }

    public void signup(String email, String password, String password2, final Boolean autoLogin) {
        Log.v(LOG_TAG, "Signing up with Email: " + email + " Password: " + password + " Password2: " + password2);
        if(!password.equals(password2)) {
            getHandler().onSignupUserError(new FirebaseSignupError(FirebaseResponse.SIGNUP_PASSWORD_NOMATCH, "Password fields do not match."));
            return;
        }

        if(autoLogin) {
            myEmail = email;
            myPassword = password;
        }

        getFirebaseRef().createUser(email, password, new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                getHandler().onSignupSuccess(result);
                if(autoLogin) {
                    login(myEmail, myPassword);
                }
            }
            @Override
            public void onError(FirebaseError firebaseError) {
                getHandler().onSignupUserError(new FirebaseSignupError(FirebaseResponse.MISC_PROVIDER_ERROR, firebaseError.toString()));
            }
        });
    }

    public void login(String email, String password) {
        myEmail = null;
        myPassword = null;

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