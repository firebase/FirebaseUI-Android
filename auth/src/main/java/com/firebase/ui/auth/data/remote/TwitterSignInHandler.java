package com.firebase.ui.auth.data.remote;

import android.app.Application;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.ui.ActivityResult;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.firebase.ui.auth.util.ui.ViewModelBase;
import com.google.firebase.auth.TwitterAuthProvider;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

public class TwitterSignInHandler extends ViewModelBase<TwitterSignInHandler.Params>
        implements Observer<ActivityResult> {
    private SignInHandler mHandler;
    private FlowHolder mFlowHolder;

    private final TwitterAuthClient mClient;
    private final Callback mCallback = new Callback();

    public TwitterSignInHandler(Application application) {
        super(application);
        initialize(getApplication());
        mClient = new TwitterAuthClient();
    }

    public static void signOut(Context context) {
        try {
            signOut();
        } catch (IllegalStateException e) {
            initialize(context);
        }

        signOut();
    }

    private static void initialize(Context context) {
        TwitterConfig config = new TwitterConfig.Builder(context)
                .twitterAuthConfig(new TwitterAuthConfig(
                        context.getString(R.string.twitter_consumer_key),
                        context.getString(R.string.twitter_consumer_secret)))
                .build();
        Twitter.initialize(config);
    }

    private static void signOut() throws IllegalStateException {
        TwitterCore.getInstance().getSessionManager().clearActiveSession();
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
    protected void onCreate(Params params) {
        mHandler = params.signInHandler;
        mFlowHolder = params.flowHolder;

        mFlowHolder.getActivityResultListener().observeForever(this);
    }

    @Override
    public void onChanged(@Nullable ActivityResult result) {
        mClient.onActivityResult(result.getRequestCode(), result.getResultCode(), result.getData());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mFlowHolder.getActivityResultListener().removeObserver(this);
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
                            mHandler.start(createIdpResponse(
                                    sessionResult.data,
                                    user.email,
                                    user.name,
                                    Uri.parse(user.profileImageUrlHttps)));
                        }

                        @Override
                        public void failure(TwitterException e) {
                            mHandler.start(IdpResponse.fromError(e));
                        }
                    });
        }

        @Override
        public void failure(TwitterException e) {
            mHandler.start(IdpResponse.fromError(e));
        }
    }

    public static final class Params {
        public final SignInHandler signInHandler;
        public final FlowHolder flowHolder;

        public Params(SignInHandler handler, FlowHolder holder) {
            signInHandler = handler;
            flowHolder = holder;
        }
    }
}
