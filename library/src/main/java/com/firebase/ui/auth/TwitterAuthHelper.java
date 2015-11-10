package com.firebase.ui.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

public class TwitterAuthHelper extends FirebaseAuthHelper {

    public static final String TAG = "TwitterAuthHelper";
    public static final String PROVIDER_NAME = "twitter";

    private Activity mActivity;
    private TokenAuthHandler mHandler;
    private Firebase mRef;

    public TwitterAuthHelper(Context context, Firebase ref, TokenAuthHandler handler) {
        mActivity = (Activity) context;
        mHandler = handler;
        mRef = ref;
    }

    public void login() {
        mActivity.startActivityForResult(new Intent(mActivity, TwitterPromptActivity.class), FirebaseStatuses.LOGIN);
    }

    public void logout() {
        // We don't store auth state in this handler, so no need to logout
    }

    public String getProviderName() { return PROVIDER_NAME; }
    public Firebase getFirebaseRef() { return mRef; }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == FirebaseStatuses.SUCCESS) {
            FirebaseOAuthToken token = new FirebaseOAuthToken(
                    PROVIDER_NAME,
                    data.getStringExtra("oauth_token"),
                    data.getStringExtra("oauth_token_secret"),
                    data.getStringExtra("user_id"));
            onFirebaseTokenReceived(token, mHandler);
        } else if (resultCode == FirebaseStatuses.USER_ERROR) {
            mHandler.onUserError(new FirebaseError(0, data.getStringExtra("error")));
        } else if (resultCode == FirebaseStatuses.PROVIDER_ERROR) {
            mHandler.onProviderError(new FirebaseError(0, data.getStringExtra("error")));
        }
    }
}