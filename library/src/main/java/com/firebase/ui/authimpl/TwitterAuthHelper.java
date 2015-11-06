package com.firebase.ui.authimpl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

public class TwitterAuthHelper extends FirebaseAuthHelper {

    public static final String TAG = "TwitterAuthHelper";
    public static final String PROVIDER_NAME = "twitter";
    public static final int RC_TWITTER_LOGIN = 1;
    public static final int RC_TWITTER_CANCEL = 2;
    public static final int RC_TWITTER_ERROR = 3;

    private Activity mActivity;
    private TokenAuthHandler mHandler;
    private Firebase mRef;

    public TwitterAuthHelper(Context context, Firebase ref, TokenAuthHandler handler) {
        mActivity = (Activity) context;
        mHandler = handler;
        mRef = ref;
    }

    public void login() {
        mActivity.startActivityForResult(new Intent(mActivity, TwitterPromptActivity.class), RC_TWITTER_LOGIN);
    }

    public void logout() {

    }

    public String getProviderName() { return PROVIDER_NAME; }
    public Firebase getFirebaseRef() { return mRef; }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RC_TWITTER_LOGIN) {
            FirebaseOAuthToken token = new FirebaseOAuthToken(
                    PROVIDER_NAME,
                    data.getStringExtra("oauth_token"),
                    data.getStringExtra("oauth_token_secret"),
                    data.getStringExtra("user_id"));
            onFirebaseTokenReceived(token, mHandler);
        } else if (resultCode == RC_TWITTER_CANCEL) {
            mHandler.onUserError(new FirebaseError(0, "user_cancel"));
            //mHandler.onCancelled();
        } else if (resultCode == RC_TWITTER_ERROR) {
            mHandler.onUserError(new FirebaseError(0, "user_error"));
        }
    }
}