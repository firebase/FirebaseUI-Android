package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

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

public class TwitterProvider extends Callback<TwitterSession> implements IDPProvider {
    public static final String EXTRA_AUTH_TOKEN = "extra_auth_token";
    public static final String EXTRA_AUTH_SECRET = "extra_auth_secret";
    private static final String EXTRA_CONSUMER_KEY = "extra_consumer_key";
    private static final String EXTRA_CONSUMER_SECRET = "extra_consumer_secret";
    private IDPCallback mCallbackObject;
    private TwitterAuthClient mTwitterAuthClient;

    public TwitterProvider(Context appContext, IDPProviderParcel twitterParcel) {
        TwitterAuthConfig authConfig = new TwitterAuthConfig(
                appContext.getString(R.string.twitter_consumer_key),
                appContext.getString(R.string.twitter_consumer_secret));
        Fabric.with(appContext, new Twitter(authConfig));
        mTwitterAuthClient = new TwitterAuthClient();
    }

    public static IDPProviderParcel createTwitterParcel(String consumerKey, String consumerSecret) {
        Bundle extra = new Bundle();
        extra.putString(EXTRA_CONSUMER_KEY, consumerKey);
        extra.putString(EXTRA_CONSUMER_SECRET, consumerSecret);
        return new IDPProviderParcel(TwitterAuthProvider.PROVIDER_ID, extra);
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
    public void setAuthenticationCallback(IDPCallback callback) {
        this.mCallbackObject = callback;
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
    public void success(Result<TwitterSession> result) {
        mCallbackObject.onSuccess(createIDPResponse(result.data));
    }

    @Override
    public void failure(TwitterException exception) {
        mCallbackObject.onFailure(new Bundle());
    }

    public static AuthCredential createAuthCredential(IDPResponse response) {
        if (!response.getProviderType().equalsIgnoreCase(TwitterAuthProvider.PROVIDER_ID)){
            return null;
        }
        return TwitterAuthProvider.getCredential(
                response.getResponse().getString(EXTRA_AUTH_TOKEN),
                response.getResponse().getString(EXTRA_AUTH_SECRET));
    }


    private IDPResponse createIDPResponse(TwitterSession twitterSession) {
        Bundle response = new Bundle();
        response.putString(EXTRA_AUTH_TOKEN, twitterSession.getAuthToken().token);
        response.putString(EXTRA_AUTH_SECRET, twitterSession.getAuthToken().secret);
        return new IDPResponse(TwitterAuthProvider.PROVIDER_ID, twitterSession.getUserName(), response);
    }

}
