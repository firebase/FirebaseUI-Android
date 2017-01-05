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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.util.GoogleApiConstants;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

public class GoogleProvider implements
        IdpProvider, OnClickListener, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "GoogleProvider";
    private static final int RC_SIGN_IN = 20;
    private static final String ERROR_KEY = "error";

    private GoogleApiClient mGoogleApiClient;
    private Activity mActivity;
    private IdpCallback mIDPCallback;

    public GoogleProvider(FragmentActivity activity, IdpConfig idpConfig) {
        this(activity, idpConfig, null);
    }

    public GoogleProvider(FragmentActivity activity, IdpConfig idpConfig, @Nullable String email) {
        mActivity = activity;
        String clientId = activity.getString(R.string.default_web_client_id);

        GoogleSignInOptions.Builder builder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken(clientId);

        if (activity.getResources().getIdentifier(
                "google_permissions", "array", activity.getPackageName()) != 0) {
            Log.w(TAG, "DEVELOPER WARNING: You have defined R.array.google_permissions but that is"
                    + " no longer respected as of FirebaseUI 1.0.0. Please see README for IDP scope"
                    + " configuration instructions.");
        }

        // Add additional scopes
        for (String scopeString : idpConfig.getScopes()) {
            builder.requestScopes(new Scope(scopeString));
        }

        if (!TextUtils.isEmpty(email)) {
            builder.setAccountName(email);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .enableAutoManage(activity, GoogleApiConstants.AUTO_MANAGE_ID0, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, builder.build())
                .build();
    }

    public static AuthCredential createAuthCredential(IdpResponse response) {
        return GoogleAuthProvider.getCredential(response.getIdpToken(), null);
    }

    public String getName(Context context) {
        return context.getResources().getString(R.string.idp_name_google);
    }

    @Override
    public String getProviderId() {
        return GoogleAuthProvider.PROVIDER_ID;
    }

    @Override
    public void setAuthenticationCallback(IdpCallback callback) {
        mIDPCallback = callback;
    }

    public void disconnect() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
    }

    private IdpResponse createIdpResponse(GoogleSignInAccount account) {
        return new IdpResponse(
                GoogleAuthProvider.PROVIDER_ID, account.getEmail(), account.getIdToken());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result != null) {
                if (result.isSuccess()) {
                    mIDPCallback.onSuccess(createIdpResponse(result.getSignInAccount()));
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
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void onError(GoogleSignInResult result) {
        String errorMessage = result.getStatus().getStatusMessage();
        onError(result.getStatus().getStatusCode() + " " + errorMessage);
    }

    private void onError(String errorMessage) {
        Log.e(TAG, "Error logging in with Google. " + errorMessage);
        Bundle extra = new Bundle();
        extra.putString(ERROR_KEY, errorMessage);
        mIDPCallback.onFailure(extra);
    }

    @Override
    public void onClick(View view) {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        startLogin(mActivity);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed:" + connectionResult);
    }
}

