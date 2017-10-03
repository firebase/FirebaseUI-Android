package com.firebase.ui.auth.provider;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.User;
import com.firebase.ui.auth.util.ActivityResultHandler;
import com.firebase.ui.auth.util.GoogleApiHelper;
import com.firebase.ui.auth.util.ViewModelBase;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.GoogleAuthProvider;

public class GoogleSignInHandler extends ViewModelBase<AuthUI.IdpConfig>
        implements ActivityResultHandler {
    public static final int RC_SIGN_IN = 20;

    private AuthUI.IdpConfig mConfig;

    public GoogleSignInHandler(Application application) {
        super(application);
    }

    @Override
    protected void onCreate(AuthUI.IdpConfig args) {
        mConfig = args;
    }

    public GoogleSignInOptions getSignInOptions(@Nullable String email) {
        String clientId = getApplication().getString(R.string.default_web_client_id);

        GoogleSignInOptions.Builder builder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken(clientId);

        // Add additional scopes
        for (String scopeString : mConfig.getScopes()) {
            builder.requestScopes(new Scope(scopeString));
        }

        if (!TextUtils.isEmpty(email)) {
            builder.setAccountName(email);
        }

        return builder.build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result != null) {
                if (result.isSuccess()) {
                    mIdpCallback.onSuccess(createIdpResponse(result.getSignInAccount()));
                } else {
                    onError(result);
                }
            } else {
                onError("No result found in intent");
            }
        }
    }

    private void onError(GoogleSignInResult result) {
        Status status = result.getStatus();

        if (status.getStatusCode() == CommonStatusCodes.INVALID_ACCOUNT) {
            mGoogleApiClient.stopAutoManage(mActivity);
            mGoogleApiClient.disconnect();
            mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                    .enableAutoManage(mActivity, GoogleApiHelper.getSafeAutoManageId(), this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, getSignInOptions(null))
                    .build();
            startLogin(mActivity);
        } else {
            if (status.getStatusCode() == CommonStatusCodes.DEVELOPER_ERROR) {
                Log.w(TAG, "Developer error: this application is misconfigured. Check your SHA1 " +
                        " and package name in the Firebase console.");
                Toast.makeText(mActivity, "Developer error.", Toast.LENGTH_SHORT).show();
            }
            onError(status.getStatusCode() + " " + status.getStatusMessage());
        }
    }

    private void onError(String errorMessage) {
        Log.e(TAG, "Error logging in with Google. " + errorMessage);
        mIdpCallback.onFailure();
    }

    private IdpResponse createIdpResponse(GoogleSignInAccount account) {
        return new IdpResponse.Builder(
                new User.Builder(GoogleAuthProvider.PROVIDER_ID, account.getEmail())
                        .setName(account.getDisplayName())
                        .setPhotoUri(account.getPhotoUrl())
                        .build())
                .setToken(account.getIdToken())
                .build();
    }
}
