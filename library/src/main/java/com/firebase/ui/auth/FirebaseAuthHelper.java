package com.firebase.ui.auth;

import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.HashMap;
import java.util.Map;

public abstract class FirebaseAuthHelper {
    public abstract void logout();
    public abstract String getProviderName();
    public abstract Firebase getFirebaseRef();

    public void login() {
        Log.d("FirebaseAuthHelper", "Login() is not supported for provider type " + getProviderName());
    };
    public void login(String email, String password) {
        Log.d("FirebaseAuthHelper", "Login(String email, String password) is not supported for provider type " + getProviderName());
    };

    public void onFirebaseTokenReceived(FirebaseOAuthToken token, TokenAuthHandler handler) {
        authenticateRefWithOAuthFirebasetoken(token, handler);
    }

    private void authenticateRefWithOAuthFirebasetoken(FirebaseOAuthToken token, final TokenAuthHandler handler) {
        Firebase.AuthResultHandler authResultHandler = new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                // Do nothing. Auth updates are handled in the AuthStateListener
                handler.onSuccess(authData);
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                handler.onProviderError(new FirebaseError(0, "Make sure " + getProviderName() + " login is enabled and configured in your Firebase."));
            }
        };

        if (token.mode == FirebaseOAuthToken.SIMPLE) {
            // Simple mode is used for Facebook and Google auth
            getFirebaseRef().authWithOAuthToken(token.provider, token.token, authResultHandler);
        } else if (token.mode == FirebaseOAuthToken.COMPLEX) {
            // Complex mode is used for Twitter auth
            Map<String, String> options = new HashMap<>();
            options.put("oauth_token", token.token);
            options.put("oauth_token_secret", token.secret);
            options.put("user_id", token.uid);

            getFirebaseRef().authWithOAuthToken(token.provider, options, authResultHandler);
        }
    }
}
