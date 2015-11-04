package com.firebase.ui.com.firebasei.ui.authimpl;

/**
 * Created by abehaskins on 11/3/15.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class TwitterAuthHelper {

    public static final String TAG = "TwitterAuthHelper";
    public final String PROVIDER_NAME = "twitter";
    public static final int RC_TWITTER_LOGIN = 1;
    public static final int RC_TWITTER_CANCEL = 2;
    public static final int RC_TWITTER_ERROR = 3;

    private Activity mActivity;
    private TokenAuthHandler mHandler;

    public TwitterAuthHelper(Context context, TokenAuthHandler handler) {
        // setup twitter client
        mActivity = (Activity) context;
        mHandler = handler;
    }

    public void login() {
        mActivity.startActivityForResult(new Intent(mActivity, TwitterPromptActivity.class), RC_TWITTER_LOGIN);
    }

    public void logout() {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RC_TWITTER_LOGIN) {
            Log.d(TAG, "Login success");
            FirebaseOAuthToken token = new FirebaseOAuthToken(
                    PROVIDER_NAME,
                    data.getStringExtra("oauth_token"),
                    data.getStringExtra("oauth_token_secret"),
                    data.getStringExtra("user_id"));
            mHandler.onTokenReceived(token);
        } else if (resultCode == RC_TWITTER_CANCEL) {
            Log.d(TAG, "Login cancel");
            mHandler.onCancelled();
        } else if (resultCode == RC_TWITTER_ERROR) {
            Log.d(TAG, "Login error");
            mHandler.onError(new Exception("Unknown Twitter Error"));
        }
    }
}