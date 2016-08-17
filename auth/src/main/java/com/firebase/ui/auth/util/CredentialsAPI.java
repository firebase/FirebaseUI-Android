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

package com.firebase.ui.auth.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class CredentialsAPI implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final int RC_CREDENTIALS_READ = 2;
    private static final String TAG = "CredentialsAPI";

    private GoogleApiClient mGoogleApiClient;
    private boolean mAutoSignInAvailable;
    private boolean mSignInResolutionNeeded;
    private Activity mActivity;
    private CredentialRequestResult mCredentialRequestResult;
    private ProgressDialog mProgressDialog;
    private Credential mCredential;
    private final CallbackInterface mCallback;
    private PlayServicesHelper mPlayServicesHelper;

    public interface CallbackInterface {
        void onAsyncTaskFinished();
    }

    public CredentialsAPI(Activity activity, CallbackInterface callback) {
        mAutoSignInAvailable = false;
        mSignInResolutionNeeded = false;
        mActivity = activity;
        mCallback = callback;
        mPlayServicesHelper = PlayServicesHelper.getInstance(mActivity);

        initGoogleApiClient(null);
        requestCredentials(true /* shouldResolve */, false /* onlyPasswords */);
    }

    public boolean isPlayServicesAvailable() {
        return mPlayServicesHelper.isPlayServicesAvailable();
    }

    public boolean isCredentialsAvailable() {
        // TODO: (serikb) find the way to check if Credentials is available on top of play services
        return true;
    }

    public boolean isAutoSignInAvailable() {
        return mAutoSignInAvailable;
    }

    public boolean isSignInResolutionNeeded() {
        return mSignInResolutionNeeded;
    }

    public void resolveSignIn() {
        mSignInResolutionNeeded = false;
    }

    public void resolveSavedEmails(Activity activity) {
        if (mCredentialRequestResult == null || mCredentialRequestResult.getStatus() == null) {
            return;
        }
        Status status = mCredentialRequestResult.getStatus();
        if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
            try {
                status.startResolutionForResult(activity, RC_CREDENTIALS_READ);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Failed to send Credentials intent.", e);
            }
        }
    }

    public Credential getCredential() {
        return mCredential;
    }

    public String getEmailFromCredential() {
        if (mCredential == null) {
            return null;
        }
        return mCredential.getId();
    }

    public String getAccountTypeFromCredential() {
        if (mCredential == null) {
            return null;
        }
        return mCredential.getAccountType();
    }

    public String getPasswordFromCredential() {
        if (mCredential == null) {
            return null;
        }
        return mCredential.getPassword();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {}

    @Override
    public void onConnectionSuspended(int cause) {}

    private void initGoogleApiClient(String accountName) {
        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();

        if (accountName != null) {
            gsoBuilder.setAccountName(accountName);
        }

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(mActivity)
                .addConnectionCallbacks(this)
                .addApi(Auth.CREDENTIALS_API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gsoBuilder.build());

        mGoogleApiClient = builder.build();
    }

    public void googleSilentSignIn() {
        // Try silent sign-in with Google Sign In API
        Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
    }

    public void handleCredential(Credential credential) {
        mCredential = credential;

        if (IdentityProviders.GOOGLE.equals(credential.getAccountType())) {
            // Google account, rebuild GoogleApiClient to set account name and then try
            initGoogleApiClient(credential.getId());
            googleSilentSignIn();
        } else {
            // Email/password account
            String status = String.format("Signed in as %s", credential.getId());
            Log.d(TAG, status);
        }
    }

    public void requestCredentials(final boolean shouldResolve, boolean onlyPasswords) {
        if (!mPlayServicesHelper.isPlayServicesAvailable()) {
            // TODO(samstern): it would probably be better to not actually call the method
            // in this case.
            return;
        }

        CredentialRequest.Builder crBuilder = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true);

        if (!onlyPasswords) {
            crBuilder.setAccountTypes(IdentityProviders.GOOGLE);
        }

        showProgress();
        Auth.CredentialsApi.request(mGoogleApiClient, crBuilder.build())
                .setResultCallback(
                        new ResultCallback<CredentialRequestResult>() {
                            @Override
                            public void onResult(CredentialRequestResult credentialRequestResult) {
                                mCredentialRequestResult = credentialRequestResult;
                                Status status = credentialRequestResult.getStatus();

                                if (status.isSuccess()) {
                                    // Auto sign-in success
                                    mAutoSignInAvailable = true;
                                    handleCredential(credentialRequestResult.getCredential());
                                } else if (status.getStatusCode() ==
                                        CommonStatusCodes.RESOLUTION_REQUIRED && shouldResolve) {
                                    mSignInResolutionNeeded = true;
                                    // Getting credential needs to show some UI, start resolution
                                }
                                hideProgress();
                                mCallback.onAsyncTaskFinished();
                            }
                        });
    }

    private void showProgress() {
        if (mProgressDialog == null || !mProgressDialog.isShowing()) {
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(
                    mActivity.getString(com.firebase.ui.auth.R.string.progress_dialog_loading));
        }
        mProgressDialog.show();
    }

    private void hideProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public void onStart() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    public void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();;
        }

        hideProgress();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(mActivity, "An error has occurred.", Toast.LENGTH_SHORT).show();
    }

    public boolean isGoogleApiClient() {
        return mGoogleApiClient != null;
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public void signOut() {
        disableAutoSignIn();
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        mCallback.onAsyncTaskFinished();
                    }
                });
    }

    public void disableAutoSignIn() {
        Auth.CredentialsApi.disableAutoSignIn(mGoogleApiClient);
    }
}
