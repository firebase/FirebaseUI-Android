package com.firebase.ui.auth.core;

import android.content.Context;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.HashMap;
import java.util.Map;

public abstract class FirebaseAuthProvider {
    private static final String TAG = "FirebaseAuthProvider";
    private final Context mContext;
    private final SocialProvider mProviderType;
    private final String mProviderName;
    private final Firebase mRef;
    private final TokenAuthHandler mHandler;

    public abstract void logout();
    public Context getContext() { return mContext; }
    public SocialProvider getProviderType() { return mProviderType; }
    public String getProviderName() { return mProviderName; }
    public Firebase getFirebaseRef() { return mRef; }
    public TokenAuthHandler getHandler() { return mHandler; }

    protected FirebaseAuthProvider(Context context, SocialProvider providerType, String providerName, Firebase ref, TokenAuthHandler handler) {
        mContext = context;
        mProviderType = providerType;
        mProviderName = providerName;
        mRef = ref;
        mHandler = handler;
    }

    public void login() {
        Log.d("FirebaseAuthProvider", "Login() is not supported for provider type " + getProviderName());
    };
    public void login(String email, String password) {
        Log.d("FirebaseAuthProvider", "Login(String email, String password) is not supported for provider type " + getProviderName());
    };

    public void onFirebaseTokenReceived(FirebaseOAuthToken token, TokenAuthHandler handler) {
        authenticateRefWithOAuthFirebasetoken(token, handler);
    }

    private void authenticateRefWithOAuthFirebasetoken(FirebaseOAuthToken token, final TokenAuthHandler handler) {
        Firebase.AuthResultHandler authResultHandler = new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                handler.onSuccess(authData);
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Log.e(TAG, "Authentication failed: "+firebaseError.toString());
                handler.onProviderError(new FirebaseLoginError(FirebaseResponse.PROVIDER_NOT_ENABLED, "Make sure " + getProviderName() + " login is enabled and configured in your Firebase. ("+firebaseError.toString()+")"));
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
