package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
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

import java.lang.ref.WeakReference;


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

    public static void initialize(Context context) {
        TwitterAuthConfig authConfig = new TwitterAuthConfig(
                context.getString(R.string.twitter_consumer_key),
                context.getString(R.string.twitter_consumer_secret));
        TwitterConfig config = new TwitterConfig.Builder(context)
                .twitterAuthConfig(authConfig)
                .build();
        Twitter.initialize(config);
    }

    public static void signout(Context context) {
        try {
            Twitter.getInstance();
        } catch (IllegalStateException e) {
            initialize(context);
        }

        signOut();
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.idp_name_twitter);
    }

    @Override
    @AuthUI.SupportedProvider
    public String getProviderId() {
        return TwitterAuthProvider.PROVIDER_ID;
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.idp_button_twitter;
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
    public void success(Result<TwitterSession> result) {
        mTwitterAuthClient.requestEmail(result.data,
                                        new EmailCallback(result.data, mCallbackObject));
    }

    @Override
    public void failure(TwitterException exception) {
        Log.e(TAG, "Failure logging in to Twitter. " + exception.getMessage());
        mCallbackObject.onFailure(new Bundle());
    }

    private static class EmailCallback extends Callback<String> {
        private TwitterSession mTwitterSession;
        private WeakReference<IdpCallback> mCallbackObject;

        public EmailCallback(TwitterSession session, IdpCallback callbackObject) {
            mTwitterSession = session;
            mCallbackObject = new WeakReference<>(callbackObject);
        }

        @Override
        public void success(Result<String> emailResult) {
            onSuccess(createIdpResponse(emailResult.data));
        }

        @Override
        public void failure(TwitterException exception) {
            Log.e(TAG, "Failure retrieving Twitter email. " + exception.getMessage());
            // If retrieving the email fails, we should still be able to sign in, but Smart Lock
            // and account linking won't work.
            onSuccess(createIdpResponse(null));
        }

        private void onSuccess(IdpResponse response) {
            if (mCallbackObject != null) {
                mCallbackObject.get().onSuccess(response);
            }
        }

        private IdpResponse createIdpResponse(String email) {
            return new IdpResponse.Builder(TwitterAuthProvider.PROVIDER_ID, email)
                    .setToken(mTwitterSession.getAuthToken().token)
                    .setSecret(mTwitterSession.getAuthToken().secret)
                    .build();
        }
    }

    private static void signOut() throws IllegalStateException {
        TwitterCore.getInstance().getSessionManager().clearActiveSession();
    }
}
