package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.TwitterAuthProvider;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

import io.fabric.sdk.android.Fabric;

public class TwitterProvider extends Callback<TwitterSession> implements IdpProvider {
    private static final String TAG = "TwitterProvider";

    private IdpCallback mCallbackObject;
    private TwitterAuthClient mTwitterAuthClient;

    public TwitterProvider(Context appContext) {
        TwitterAuthConfig authConfig = new TwitterAuthConfig(
                appContext.getString(R.string.twitter_consumer_key),
                appContext.getString(R.string.twitter_consumer_secret));
        Fabric.with(appContext.getApplicationContext(), new Twitter(authConfig));
        mTwitterAuthClient = new TwitterAuthClient();
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.idp_name_twitter);
    }

    @Override
    public String getProviderId() {
        return TwitterAuthProvider.PROVIDER_ID;
    }

    @Override
    public void setAuthenticationCallback(IdpCallback callback) {
        mCallbackObject = callback;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mTwitterAuthClient.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void startLogin(Activity activity) {
        mTwitterAuthClient.authorize(activity, this);
    }

    @Override
    public void success(final Result<TwitterSession> sessionResult) {
        mTwitterAuthClient.requestEmail(sessionResult.data, new Callback<String>() {
            @Override
            public void success(Result<String> result) {
                mCallbackObject.onSuccess(createIdpResponse(sessionResult.data, result.data));
            }

            @Override
            public void failure(TwitterException exception) {
                TwitterProvider.this.failure(exception);
            }
        });
    }

    @Override
    public void failure(TwitterException exception) {
        Log.e(TAG, "Failure logging in to Twitter. " + exception.getMessage());
        mCallbackObject.onFailure(new Bundle());
    }

    private IdpResponse createIdpResponse(TwitterSession twitterSession, String email) {
        return new IdpResponse(
                TwitterAuthProvider.PROVIDER_ID,
                email,
                twitterSession.getAuthToken().token,
                twitterSession.getAuthToken().secret);
    }

    public static AuthCredential createAuthCredential(IdpResponse response) {
        if (!response.getProviderType().equalsIgnoreCase(TwitterAuthProvider.PROVIDER_ID)){
            return null;
        }
        return TwitterAuthProvider.getCredential(
                response.getIdpToken(),
                response.getIdpSecret());
    }
}
