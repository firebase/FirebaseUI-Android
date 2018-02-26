/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.ExtraConstants;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

public class GoogleProvider implements IdpProvider {
    private static final String TAG = "GoogleProvider";
    private static final int RC_SIGN_IN = 20;

    private GoogleSignInClient mSignInClient;
    private FragmentActivity mActivity;
    private IdpConfig mIdpConfig;
    private IdpCallback mIdpCallback;
    private boolean mSpecificAccount;

    public GoogleProvider(FragmentActivity activity, IdpConfig idpConfig) {
        this(activity, idpConfig, null);
    }

    public GoogleProvider(FragmentActivity activity, IdpConfig idpConfig, @Nullable String email) {
        mActivity = activity;
        mIdpConfig = idpConfig;
        mSpecificAccount = !TextUtils.isEmpty(email);

        mSignInClient = GoogleSignIn.getClient(mActivity, getSignInOptions(email));
    }

    public static AuthCredential createAuthCredential(IdpResponse response) {
        return GoogleAuthProvider.getCredential(response.getIdpToken(), null);
    }

    private GoogleSignInOptions getSignInOptions(@Nullable String email) {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(
                mIdpConfig.getParams().<GoogleSignInOptions>getParcelable(
                        ExtraConstants.EXTRA_GOOGLE_SIGN_IN_OPTIONS));

        if (!TextUtils.isEmpty(email)) {
            builder.setAccountName(email);
        }

        return builder.build();
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.fui_idp_name_google);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_idp_button_google;
    }

    @Override
    public void setAuthenticationCallback(IdpCallback callback) {
        mIdpCallback = callback;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result != null) {
                if (result.isSuccess()) {
                    if (mSpecificAccount) {
                        Toast.makeText(
                                mActivity,
                                mActivity.getString(
                                        R.string.fui_signed_in_with_specific_account,
                                        result.getSignInAccount().getEmail()),
                                Toast.LENGTH_SHORT).show();
                    }
                    mIdpCallback.onSuccess(createIdpResponse(result.getSignInAccount()));
                } else {
                    onError(result);
                }
            } else {
                onError("No result found in intent");
            }
        }
    }

    @Override
    public void startLogin(Activity activity) {
        Intent signInIntent = mSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void onError(GoogleSignInResult result) {
        Status status = result.getStatus();

        if (status.getStatusCode() == CommonStatusCodes.INVALID_ACCOUNT) {
            mSignInClient = GoogleSignIn.getClient(mActivity, getSignInOptions(null));
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
        mIdpCallback.onFailure(new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR, errorMessage));
    }
}

