package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.LayoutRes;
import android.util.Log;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.TwitterAuthProvider;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.twitter.sdk.android.core.models.User;

public class TwitterProvider extends Callback<TwitterSession> implements IdpProvider {
    private static final String TAG = "TwitterProvider";

    private IdpCallback mCallbackObject;
    private TwitterAuthClient mTwitterAuthClient;

    public TwitterProvider(Context context) {
        initialize(context);
        mTwitterAuthClient = new TwitterAuthClient();
    }

    public static AuthCredential createAuthCredential(IdpResponse response) {
        if (!response.getProviderType().equalsIgnoreCase(TwitterAuthProvider.PROVIDER_ID)) {
            return null;
        }
        return TwitterAuthProvider.getCredential(response.getIdpToken(), response.getIdpSecret());
    }

    private static void initialize(Context context) {
        TwitterAuthConfig authConfig = new TwitterAuthConfig(
                context.getString(R.string.twitter_consumer_key),
                context.getString(R.string.twitter_consumer_secret));
        TwitterConfig config = new TwitterConfig.Builder(context)
                .twitterAuthConfig(authConfig)
                .build();
        Twitter.initialize(config);
    }

    public static void signOut(Context context) {
        try {
            Twitter.getInstance();
        } catch (IllegalStateException e) {
            initialize(context);
        }

        signOut();
    }

    private static void signOut() throws IllegalStateException {
        TwitterCore.getInstance().getSessionManager().clearActiveSession();
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.fui_idp_name_twitter);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_idp_button_twitter;
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
        TwitterCore.getInstance()
                .getApiClient()
                .getAccountService()
                .verifyCredentials(false, false, true)
                .enqueue(new Callback<User>() {
                    @Override
                    public void success(Result<User> result) {
                        User user = result.data;
                        mCallbackObject.onSuccess(createIdpResponse(
                                sessionResult.data,
                                user.email,
                                user.name,
                                Uri.parse(user.profileImageUrlHttps)));
                    }

                    @Override
                    public void failure(TwitterException e) {
                        mCallbackObject.onFailure(e);
                    }
                });
    }

    @Override
    public void failure(TwitterException e) {
        Log.e(TAG, "Failure logging in to Twitter. " + e.getMessage());
        mCallbackObject.onFailure(e);
    }

    private IdpResponse createIdpResponse(TwitterSession session,
                                          String email,
                                          String name,
                                          Uri photoUri) {
        return new IdpResponse.Builder(
                new com.firebase.ui.auth.data.model.User.Builder(TwitterAuthProvider.PROVIDER_ID,
                        email)
                        .setName(name)
                        .setPhotoUri(photoUri)
                        .build())
                .setToken(session.getAuthToken().token)
                .setSecret(session.getAuthToken().secret)
                .build();
    }
}
