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
import com.google.gson.GsonBuilder;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.twitter.sdk.android.core.internal.TwitterApi;
import com.twitter.sdk.android.core.internal.network.OkHttpClientHelper;
import com.twitter.sdk.android.core.models.BindingValues;
import com.twitter.sdk.android.core.models.BindingValuesAdapter;
import com.twitter.sdk.android.core.models.SafeListAdapter;
import com.twitter.sdk.android.core.models.SafeMapAdapter;
import com.twitter.sdk.android.core.models.User;

import io.fabric.sdk.android.Fabric;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

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
        new Retrofit.Builder()
                .client(getClient(sessionResult))
                .baseUrl(new TwitterApi().getBaseHostUrl())
                .addConverterFactory(getFactory())
                .build()
                .create(EmailService.class)
                .getEmail()
                .enqueue(new Callback<User>() {
                    @Override
                    public void success(Result<User> result) {
                        String email = result.data.email;
                        if (email == null) {
                            TwitterProvider.this.failure(
                                    new TwitterException("Your application may not have access to"
                                                                 + " email addresses or the user may not have an email address. To request"
                                                                 + " access, please visit https://support.twitter.com/forms/platform."));
                        } else if (email.equals("")) {
                            TwitterProvider.this.failure(
                                    new TwitterException("This user does not have an email address."));
                        } else {
                            mCallbackObject.onSuccess(createIdpResponse(sessionResult.data, email));
                        }
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

    private OkHttpClient getClient(Result<TwitterSession> sessionResult) {
        return OkHttpClientHelper.getOkHttpClient(
                sessionResult.data,
                TwitterCore.getInstance().getAuthConfig(),
                TwitterCore.getInstance().getSSLSocketFactory());
    }

    private GsonConverterFactory getFactory() {
        return GsonConverterFactory.create(
                new GsonBuilder()
                        .registerTypeAdapterFactory(new SafeListAdapter())
                        .registerTypeAdapterFactory(new SafeMapAdapter())
                        .registerTypeAdapter(BindingValues.class, new BindingValuesAdapter())
                        .create());
    }

    interface EmailService {
        @GET("/1.1/account/verify_credentials.json?include_email=true?include_entities=true?skip_status=true")
        Call<User> getEmail();
    }

    public static AuthCredential createAuthCredential(IdpResponse response) {
        if (!response.getProviderType().equalsIgnoreCase(TwitterAuthProvider.PROVIDER_ID)) {
            return null;
        }
        return TwitterAuthProvider.getCredential(response.getIdpToken(), response.getIdpSecret());
    }
}
