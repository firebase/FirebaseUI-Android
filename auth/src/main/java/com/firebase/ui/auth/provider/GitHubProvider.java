package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.gson.JsonObject;

import java.lang.ref.WeakReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class GitHubProvider implements IdpProvider, Callback<JsonObject> {
    public static final String IDENTITY = "https://github.com";

    public static final String RESULT_CODE = "result_code";
    public static final String KEY_GITHUB_CODE = "github_code";

    private static final String GITHUB_OAUTH_BASE = "https://github.com/login/oauth/";
    private static final String AUTHORIZE_QUERY = "authorize?client_id=";
    private static final String SCOPE_QUERY = "&scope=";

    private static final GitHubOAuth RETROFIT_GITHUB_OAUTH = new Retrofit.Builder()
            .baseUrl(GITHUB_OAUTH_BASE)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubOAuth.class);
    private static final GitHubApi RETROFIT_GITHUB = new Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApi.class);

    private static final String KEY_ACCESS_TOKEN = "access_token";

    private static GitHubRedirectReceiver sReceiver;

    private final Context mContext;
    private IdpCallback mCallback;

    public static AuthCredential createAuthCredential(IdpResponse response) {
        if (!response.getProviderType().equals(GithubAuthProvider.PROVIDER_ID)) {
            return null;
        }
        return GithubAuthProvider.getCredential(response.getIdpToken());
    }

    public GitHubProvider(Context context) {
        mContext = context.getApplicationContext();

        if (sReceiver != null) handleRedirectResult(this, sReceiver.getStoredResult());
    }

    public static IntentFilter getGitHubRedirectFilter(Context context) {
        return new IntentFilter(
                context.getApplicationContext().getPackageName() + ".github_redirect");
    }

    private static void handleRedirectResult(GitHubProvider provider, Intent intent) {
        if (intent != null
                && intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED) == Activity.RESULT_OK
                && !TextUtils.isEmpty(intent.getStringExtra(KEY_GITHUB_CODE))) {
            RETROFIT_GITHUB_OAUTH.getAuthToken(
                    "application/json",
                    provider.mContext.getString(R.string.github_client_id),
                    provider.mContext.getString(R.string.github_client_secret),
                    intent.getStringExtra(KEY_GITHUB_CODE))
                    .enqueue(provider);
        } else {
            provider.mCallback.onFailure(new Bundle());
        }

        sReceiver = null;
    }

    @Override
    public void setAuthenticationCallback(IdpCallback callback) {
        mCallback = callback;
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.idp_name_github);
    }

    @Override
    public String getProviderId() {
        return GithubAuthProvider.PROVIDER_ID;
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.idp_button_github;
    }

    @Override
    public void startLogin(Activity activity) {
        sReceiver = new GitHubRedirectReceiver(this);

        LocalBroadcastManager.getInstance(mContext)
                .registerReceiver(sReceiver, getGitHubRedirectFilter(mContext));

        CustomTabsIntent intent = new CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                .build();
        intent.launchUrl(
                mContext,
                Uri.parse(GITHUB_OAUTH_BASE + AUTHORIZE_QUERY + activity.getString(R.string.github_client_id)
                                  + SCOPE_QUERY + getScopeList()));
    }

    private String getScopeList() {
        return "user:email";
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // We use a broadcast receiver for GitHub OAuth
    }

    @Override
    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
        if (response.isSuccessful()) {
            final String token = response.body().get(KEY_ACCESS_TOKEN).getAsString();

            if (response.body().get("scope").getAsString().contains("user:email")) {
                RETROFIT_GITHUB.getUser("token " + token).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        mCallback.onSuccess(
                                getIdpResponse(response.body().get("email").getAsString(), token));
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable throwable) {
                        mCallback.onSuccess(getIdpResponse(null, token));
                    }
                });
            } else {
                mCallback.onSuccess(getIdpResponse(null, token));
            }
        } else {
            mCallback.onFailure(new Bundle());
        }
    }

    private IdpResponse getIdpResponse(@Nullable String email, @NonNull String token) {
        return new IdpResponse.Builder(GithubAuthProvider.PROVIDER_ID, email)
                .setToken(token)
                .build();
    }

    @Override
    public void onFailure(Call<JsonObject> call, Throwable throwable) {
        mCallback.onFailure(new Bundle());
    }

    private interface GitHubOAuth {
        @POST(KEY_ACCESS_TOKEN)
        Call<JsonObject> getAuthToken(@Header("Accept") String header,
                                      @Query("client_id") String id,
                                      @Query("client_secret") String secret,
                                      @Query("code") String code);
    }

    private interface GitHubApi {
        @GET("user")
        Call<JsonObject> getUser(@Header("Authorization") String token);
    }

    private static class GitHubRedirectReceiver extends BroadcastReceiver {
        private final WeakReference<GitHubProvider> mGitHubProvider;

        @Nullable private Intent mStoredResult;

        public GitHubRedirectReceiver(GitHubProvider provider) {
            mGitHubProvider = new WeakReference<>(provider);
        }

        @Nullable
        private Intent getStoredResult() {
            return mStoredResult;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            GitHubProvider provider = mGitHubProvider.get();
            if (provider == null) {
                mStoredResult = intent;
                LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
                return;
            }

            handleRedirectResult(provider, intent);
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        }
    }
}
