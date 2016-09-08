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

import com.firebase.ui.auth.R;
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
        IDPProvider, OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    public static final String TOKEN_KEY = "token_key";

    private static final String TAG = "GoogleProvider";
    private static final int AUTO_MANAGE_ID = 1;
    private static final int RC_SIGN_IN = 20;
    private static final String ERROR_KEY = "error";
    private static final String CLIENT_ID_KEY = "client_id_key";
    private GoogleApiClient mGoogleApiClient;
    private Activity mActivity;
    private IDPCallback mIDPCallback;

    public GoogleProvider(FragmentActivity activity, IDPProviderParcel parcel, @Nullable String email) {
        mActivity = activity;
        String mClientId = parcel.getProviderExtra().getString(CLIENT_ID_KEY);
        GoogleSignInOptions googleSignInOptions;

        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions
                .DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(mClientId);

        // Add additional scopes
        String[] extraScopes = mActivity.getResources().getStringArray(R.array.google_permissions);
        for (String scopeString : extraScopes) {
            builder.requestScopes(new Scope(scopeString));
        }

        if (!TextUtils.isEmpty(email)) {
            builder.setAccountName(email);
        }
        googleSignInOptions = builder.build();

        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .enableAutoManage(activity, AUTO_MANAGE_ID, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build();
    }

    public String getName(Context context) {
        return context.getResources().getString(R.string.idp_name_google);
    }

    @Override
    public String getProviderId() {
        return GoogleAuthProvider.PROVIDER_ID;
    }

    public static IDPProviderParcel createParcel(String clientId) {
        Bundle extra = new Bundle();
        extra.putString(CLIENT_ID_KEY, clientId);
        return new IDPProviderParcel(GoogleAuthProvider.PROVIDER_ID, extra);
    }

    public static AuthCredential createAuthCredential(IDPResponse response) {
        Bundle bundle = response.getResponse();
        return GoogleAuthProvider.getCredential(bundle.getString(TOKEN_KEY), null);
    }

    @Override
    public void setAuthenticationCallback(IDPCallback callback) {
        mIDPCallback = callback;
    }

    public void disconnect() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
    }

    private IDPResponse createIDPResponse(GoogleSignInAccount account) {
        Bundle response = new Bundle();
        response.putString(TOKEN_KEY, account.getIdToken());
        return new IDPResponse(GoogleAuthProvider.PROVIDER_ID, account.getEmail(), response);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result != null) {
                if (result.isSuccess()) {
                    mIDPCallback.onSuccess(createIDPResponse(result.getSignInAccount()));
                } else {
                    onError(result.getStatus().getStatusMessage());
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

    private void onError(String errorMessage) {
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

