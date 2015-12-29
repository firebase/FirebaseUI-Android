package com.firebase.ui.auth.twitter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.firebase.client.Firebase;
import com.firebase.ui.auth.core.FirebaseAuthProvider;
import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.FirebaseOAuthToken;
import com.firebase.ui.auth.core.FirebaseResponse;
import com.firebase.ui.auth.core.AuthProviderType;
import com.firebase.ui.auth.core.TokenAuthHandler;

public class TwitterAuthProvider extends FirebaseAuthProvider {

    public static final String TAG = "TwitterAuthProvider";

    public TwitterAuthProvider(Context context, AuthProviderType providerType, String providerName, Firebase ref, TokenAuthHandler handler) {
        super(context, providerType, providerName, ref, handler);
    }

    public void login() {
        ((Activity)getContext()).startActivityForResult(new Intent(getContext(), TwitterPromptActivity.class), TwitterActions.REQUEST);
    }

    public void logout() {
        // We don't store auth state in this handler, so no need to logout
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == TwitterActions.SUCCESS) {
            FirebaseOAuthToken token = new FirebaseOAuthToken(
                    getProviderName(),
                    data.getStringExtra("oauth_token"),
                    data.getStringExtra("oauth_token_secret"),
                    data.getStringExtra("user_id"));
            onFirebaseTokenReceived(token, getHandler());
        } else if (resultCode == TwitterActions.USER_ERROR) {
            FirebaseResponse error = FirebaseResponse.values()[data.getIntExtra("code", 0)];
            getHandler().onUserError(new FirebaseLoginError(error, data.getStringExtra("error")));
        } else if (resultCode == TwitterActions.PROVIDER_ERROR) {
            FirebaseResponse error = FirebaseResponse.values()[data.getIntExtra("code", 0)];
            getHandler().onProviderError(new FirebaseLoginError(error, data.getStringExtra("error")));
        }
    }
}