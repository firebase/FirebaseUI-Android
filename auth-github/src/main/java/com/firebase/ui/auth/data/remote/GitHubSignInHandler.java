package com.firebase.ui.auth.data.remote;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.GitHubProfile;
import com.firebase.ui.auth.data.model.GitHubTokenResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.github.GitHubLoginActivity;
import com.firebase.ui.auth.ui.github.R;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.firebase.auth.GithubAuthProvider;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GitHubSignInHandler extends ProviderSignInBase<AuthUI.IdpConfig>
        implements Callback<GitHubTokenResponse> {
    public static final String RESULT_CODE = "result_code";
    public static final String KEY_GITHUB_CODE = "github_code";

    private static final String SCHEME = "https";
    private static final String AUTHORITY = "github.com";
    private static final String OAUTH = "login/oauth";
    private static final GitHubOAuth RETROFIT_GITHUB_OAUTH = new Retrofit.Builder()
            .baseUrl(SCHEME + "://" + AUTHORITY + "/" + OAUTH + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubOAuth.class);
    private static final GitHubApi RETROFIT_GITHUB = new Retrofit.Builder()
            .baseUrl(SCHEME + "://api." + AUTHORITY + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApi.class);

    private static final String EMAIL_PERMISSION = "user:email";

    private List<String> mPermissions;

    public GitHubSignInHandler(Application application) {
        super(application);
    }

    private static IdpResponse createIdpResponse(
            @NonNull String token,
            @NonNull GitHubProfile profile) {
        return new IdpResponse.Builder(
                new User.Builder(GithubAuthProvider.PROVIDER_ID, profile.getEmail())
                        .setName(profile.getName())
                        .setPhotoUri(profile.getAvatarUri())
                        .build())
                .setToken(token)
                .build();
    }

    @Override
    protected void onCreate() {
        List<String> permissions = getArguments().getParams()
                .getStringArrayList(ExtraConstants.GITHUB_PERMISSIONS);

        if (permissions == null) {
            permissions = new ArrayList<>();
        } else {
            permissions = new ArrayList<>(permissions);
        }

        if (!permissions.contains(EMAIL_PERMISSION)) {
            permissions.add(EMAIL_PERMISSION);
        }
        mPermissions = permissions;
    }

    @Override
    public void startSignIn(@NonNull HelperActivityBase activity) {
        activity.startActivityForResult(GitHubLoginActivity.createIntent(activity,
                new Uri.Builder().scheme(SCHEME)
                        .authority(AUTHORITY)
                        .path(OAUTH + "/authorize")
                        .appendQueryParameter("client_id",
                                getApplication().getString(R.string.github_client_id))
                        .appendQueryParameter("scope", TextUtils.join(",", mPermissions))
                        .build()),
                RequestCodes.GITHUB_PROVIDER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != RequestCodes.GITHUB_PROVIDER) { return; }

        if (data == null) {
            setResult(Resource.<IdpResponse>forFailure(new UserCancellationException()));
            return;
        }

        if (data.hasExtra(KEY_GITHUB_CODE)) {
            setResult(Resource.<IdpResponse>forLoading());
            RETROFIT_GITHUB_OAUTH.getAuthToken(
                    "application/json",
                    getApplication().getString(R.string.github_client_id),
                    getApplication().getString(R.string.github_client_secret),
                    data.getStringExtra(KEY_GITHUB_CODE)
            ).enqueue(this);
        } else {
            setResult(Resource.<IdpResponse>forFailure(new FirebaseUiException(ErrorCodes.PROVIDER_ERROR)));
        }
    }

    @Override
    public void onResponse(Call<GitHubTokenResponse> call, Response<GitHubTokenResponse> response) {
        if (response.isSuccessful()) {
            String token = response.body().getToken();
            RETROFIT_GITHUB.getUser("token " + token).enqueue(new ProfileRequest(token));
        } else {
            setResult(Resource.<IdpResponse>forFailure(new FirebaseUiException(
                    ErrorCodes.PROVIDER_ERROR, response.message())));
        }
    }

    @Override
    public void onFailure(Call<GitHubTokenResponse> call, Throwable throwable) {
        setResult(Resource.<IdpResponse>forFailure(
                new FirebaseUiException(ErrorCodes.PROVIDER_ERROR, throwable)));
    }

    private final class ProfileRequest implements Callback<GitHubProfile> {
        private final String mToken;

        public ProfileRequest(String token) {
            mToken = token;
        }

        @Override
        public void onResponse(Call<GitHubProfile> call, Response<GitHubProfile> response) {
            if (response.isSuccessful()) {
                setResult(Resource.forSuccess(createIdpResponse(mToken, response.body())));
            } else {
                onFailure(call, new FirebaseUiException(
                        ErrorCodes.PROVIDER_ERROR, response.message()));
            }
        }

        @Override
        public void onFailure(Call<GitHubProfile> call, Throwable throwable) {
            // Ignore profile request failures since we can still sign in
            setResult(Resource.forSuccess(createIdpResponse(mToken, new GitHubProfile())));
        }
    }

    private interface GitHubOAuth {
        @POST("access_token")
        Call<GitHubTokenResponse> getAuthToken(@Header("Accept") String header,
                                               @Query("client_id") String id,
                                               @Query("client_secret") String secret,
                                               @Query("code") String code);
    }

    private interface GitHubApi {
        @GET("user")
        Call<GitHubProfile> getUser(@Header("Authorization") String token);
    }
}
