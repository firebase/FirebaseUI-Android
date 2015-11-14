package com.firebase.ui.auth.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.ui.auth.core.FirebaseActions;
import com.firebase.ui.auth.core.FirebaseAuthHelper;
import com.firebase.ui.auth.core.FirebaseErrors;
import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.FirebaseOAuthToken;
import com.firebase.ui.auth.core.TokenAuthHandler;
import com.firebase.ui.auth.twitter.TwitterPromptActivity;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;

public class GoogleAuthHelper extends FirebaseAuthHelper {

    private final String LOG_TAG = "GoogleAuthHelper";

    public final static String PROVIDER_NAME = "google";

    private TokenAuthHandler mHandler;
    private Activity mActivity;
    private Firebase mRef;
    private GoogleApiClient mGoogleApiClient;

    public GoogleAuthHelper(Context context, Firebase ref, TokenAuthHandler handler) {
        mActivity = (Activity) context;
        mRef = ref;
        mHandler = handler;
    }

    public String getProviderName() { return PROVIDER_NAME; }
    public Firebase getFirebaseRef() { return mRef; }
    public void logout() {}
    public void login() {
        mActivity.startActivityForResult(new Intent(mActivity, GoogleSignInActivity.class), FirebaseActions.LOGIN);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 120321) {
            FirebaseOAuthToken token = new FirebaseOAuthToken(
                    PROVIDER_NAME,
                    data.getStringExtra("oauth_token"));
            onFirebaseTokenReceived(token, mHandler);
        } else if (resultCode == FirebaseActions.USER_ERROR) {
            mHandler.onUserError(new FirebaseLoginError(data.getIntExtra("code", 0), data.getStringExtra("error")));
        } else if (resultCode == FirebaseActions.PROVIDER_ERROR) {
            mHandler.onProviderError(new FirebaseLoginError(data.getIntExtra("code", 0), data.getStringExtra("error")));
        }
    }
}
