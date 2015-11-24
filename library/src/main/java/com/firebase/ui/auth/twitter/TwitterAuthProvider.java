package com.firebase.ui.auth.twitter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.firebase.client.Firebase;
import com.firebase.ui.auth.core.FirebaseAuthHelper;
import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.FirebaseOAuthToken;
import com.firebase.ui.auth.core.TokenAuthHandler;

public class TwitterAuthProvider extends FirebaseAuthHelper {

    public static final String TAG = "TwitterAuthProvider";
    public static final String PROVIDER_NAME = "twitter";

    private Activity mActivity;
    private TokenAuthHandler mHandler;
    private Firebase mRef;

    public TwitterAuthProvider(Context context, Firebase ref, TokenAuthHandler handler) {
        mActivity = (Activity) context;
        mHandler = handler;
        mRef = ref;
    }

    public void login() {
        mActivity.startActivityForResult(new Intent(mActivity, TwitterPromptActivity.class), TwitterActions.REQUEST);
    }

    public void logout() {
        // We don't store auth state in this handler, so no need to logout
    }

    public String getProviderName() { return PROVIDER_NAME; }
    public Firebase getFirebaseRef() { return mRef; }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == TwitterActions.SUCCESS) {
            FirebaseOAuthToken token = new FirebaseOAuthToken(
                    PROVIDER_NAME,
                    data.getStringExtra("oauth_token"),
                    data.getStringExtra("oauth_token_secret"),
                    data.getStringExtra("user_id"));
            onFirebaseTokenReceived(token, mHandler);
        } else if (resultCode == TwitterActions.USER_ERROR) {
            mHandler.onUserError(new FirebaseLoginError(data.getIntExtra("code", 0), data.getStringExtra("error")));
        } else if (resultCode == TwitterActions.PROVIDER_ERROR) {
            mHandler.onProviderError(new FirebaseLoginError(data.getIntExtra("code", 0), data.getStringExtra("error")));
        }
    }
}