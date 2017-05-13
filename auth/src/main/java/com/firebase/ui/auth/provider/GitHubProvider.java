package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.FlowParameters;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.gson.JsonObject;

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
    public static final String GITHUB_OAUTH_BASE = "https://github.com/login/oauth/";
    public static final String KEY_GITHUB_CODE = "github_code";

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

    private static final int RC_SIGN_IN = 967;
    private static final String KEY_ACCESS_TOKEN = "access_token";

    private final Context mContext;
    private final FlowParameters mFlowParameters;
    private IdpCallback mCallback;

    public static AuthCredential createAuthCredential(IdpResponse response) {
        if (!response.getProviderType().equals(GithubAuthProvider.PROVIDER_ID)) {
            return null;
        }
        return GithubAuthProvider.getCredential(response.getIdpToken());
    }

    public GitHubProvider(Context context, FlowParameters flowParameters) {
        mContext = context;
        mFlowParameters = flowParameters;
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
        activity.startActivityForResult(
                GitHubLoginHolder.createIntent(activity, mFlowParameters), RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != RC_SIGN_IN) return;

        if (resultCode != Activity.RESULT_OK ||
                data == null || TextUtils.isEmpty(data.getStringExtra(KEY_GITHUB_CODE))) {
            mCallback.onFailure(new Bundle());
        } else {
            RETROFIT_GITHUB_OAUTH.getAuthToken("application/json",
                                               mContext.getString(R.string.github_client_id),
                                               mContext.getString(R.string.github_client_secret),
                                               data.getStringExtra(KEY_GITHUB_CODE))
                    .enqueue(this);
        }
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
}
