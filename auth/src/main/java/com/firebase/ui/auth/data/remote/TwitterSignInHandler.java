package com.firebase.ui.auth.data.remote;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.viewmodel.idp.ProviderHandler;
import com.firebase.ui.auth.viewmodel.idp.ProviderParamsBase;
import com.firebase.ui.auth.viewmodel.idp.ProvidersHandlerBase;
import com.google.firebase.auth.TwitterAuthProvider;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TwitterSignInHandler extends ProviderHandler<TwitterSignInHandler.Params> {
    private final TwitterAuthClient mClient;
    private final Callback mCallback = new Callback();

    public TwitterSignInHandler(Application application) {
        super(application);
        initialize();
        mClient = new TwitterAuthClient();
    }

    public static void signOut() {
        try {
            TwitterCore.getInstance();
        } catch (IllegalStateException e) {
            initialize();
        }

        TwitterCore.getInstance().getSessionManager().clearActiveSession();
    }

    private static void initialize() {
        Context context = AuthUI.getApplicationContext();
        TwitterConfig config = new TwitterConfig.Builder(context)
                .twitterAuthConfig(new TwitterAuthConfig(
                        context.getString(R.string.twitter_consumer_key),
                        context.getString(R.string.twitter_consumer_secret)))
                .build();
        Twitter.initialize(config);
    }

    private static IdpResponse createIdpResponse(
            TwitterSession session, String email, String name, Uri photoUri) {
        return new IdpResponse.Builder(
                new User.Builder(TwitterAuthProvider.PROVIDER_ID, email)
                        .setName(name)
                        .setPhotoUri(photoUri)
                        .build())
                .setToken(session.getAuthToken().token)
                .setSecret(session.getAuthToken().secret)
                .build();
    }

    public TwitterAuthClient getClient() {
        return mClient;
    }

    public Callback getCallback() {
        return mCallback;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mClient.onActivityResult(requestCode, resultCode, data);
    }

    private class Callback extends com.twitter.sdk.android.core.Callback<TwitterSession> {
        @Override
        public void success(final Result<TwitterSession> sessionResult) {
            TwitterCore.getInstance()
                    .getApiClient()
                    .getAccountService()
                    .verifyCredentials(false, false, true)
                    .enqueue(new com.twitter.sdk.android.core.Callback<com.twitter.sdk.android.core.models.User>() {
                        @Override
                        public void success(Result<com.twitter.sdk.android.core.models.User> result) {
                            com.twitter.sdk.android.core.models.User user = result.data;
                            setResult(createIdpResponse(
                                    sessionResult.data,
                                    user.email,
                                    user.name,
                                    Uri.parse(user.profileImageUrlHttps)));
                        }

                        @Override
                        public void failure(TwitterException e) {
                            setResult(IdpResponse.fromError(e));
                        }
                    });
        }

        @Override
        public void failure(TwitterException e) {
            setResult(IdpResponse.fromError(e));
        }
    }

    public static final class Params extends ProviderParamsBase {
        public Params(ProvidersHandlerBase handler) {
            super(handler);
        }
    }
}
