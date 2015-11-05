package com.firebase.ui.com.firebasei.ui.authimpl;

import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by abehaskins on 11/4/15.
 */
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
        if (token.mode == FirebaseOAuthToken.SIMPLE) {
            // Simple mode is used for Facebook and Google auth
            getFirebaseRef().authWithOAuthToken(token.provider, token.token, new Firebase.AuthResultHandler() {
                @Override
                public void onAuthenticated(AuthData authData) {
                    // Do nothing. Auth updates are handled in the AuthStateListener
                }

                @Override
                public void onAuthenticationError(FirebaseError firebaseError) {
                    handler.onUserError(new FirebaseError(0, "auth_error"));
                }
            });
        } else if (token.mode == FirebaseOAuthToken.COMPLEX) {
            // Complex mode is used for Twitter auth
            Map<String, String> options = new HashMap<>();
            options.put("oauth_token", token.token);
            options.put("oauth_token_secret", token.secret);
            options.put("user_id", token.uid);

            getFirebaseRef().authWithOAuthToken(token.provider, options, new Firebase.AuthResultHandler() {
                @Override
                public void onAuthenticated(AuthData authData) {
                    // Do nothing. Auth updates are handled in the AuthStateListener
                }

                @Override
                public void onAuthenticationError(FirebaseError firebaseError) {
                    handler.onUserError(new FirebaseError(0, "auth_error"));
                }
            });
        }
    }
}
