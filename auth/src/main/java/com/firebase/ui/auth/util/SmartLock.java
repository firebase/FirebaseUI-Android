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

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.FlowParameters;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import static android.app.Activity.RESULT_OK;

public class SmartLock implements GoogleApiClient.ConnectionCallbacks, ResultCallback<Status>,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "CredentialsSaveBase";
    private static final int RC_SAVE = 100;
    private static final int RC_UPDATE_SERVICE = 28;

    private AppCompatBase mActivity;
    private String mName;
    private String mEmail;
    private String mPassword;
    private String mProvider;
    private String mProfilePictureUri;
    private GoogleApiClient mCredentialsApiClient;

    public SmartLock(AppCompatBase activity,
                     String name,
                     String email,
                     String password,
                     String provider,
                     String profilePictureUri) {
        mActivity = activity;
        mName = name;
        mEmail = email;
        mPassword = password;
        mProvider = provider;
        mProfilePictureUri = profilePictureUri;

        mCredentialsApiClient = new GoogleApiClient.Builder(mActivity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Auth.CREDENTIALS_API)
                .enableAutoManage(mActivity, this)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mEmail == null) {
            Log.e(TAG, "Unable to save null credential!");
            mActivity.finish(RESULT_OK, mActivity.getIntent());
            return;
        }

        Credential.Builder builder = new Credential.Builder(mEmail);
        builder.setPassword(mPassword);
        if (mPassword == null) {
            // only password OR provider can be set, not both
            if (mProvider != null) {
                String translatedProvider = null;
                // translate the google.com/facebook.com provider strings into full URIs
                if (mProvider.equals(GoogleAuthProvider.PROVIDER_ID)) {
                    translatedProvider = IdentityProviders.GOOGLE;
                } else if (mProvider.equals(FacebookAuthProvider.PROVIDER_ID)) {
                    translatedProvider = IdentityProviders.FACEBOOK;
                }
                if (translatedProvider != null) {
                    builder.setAccountType(translatedProvider);
                }
            }
        }

        if (mName != null) {
            builder.setName(mName);
        }

        if (mProfilePictureUri != null) {
            builder.setProfilePictureUri(Uri.parse(mProfilePictureUri));
        }

        Auth.CredentialsApi
                .save(mCredentialsApiClient, builder.build())
                .setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Connection suspended with code " + i);
        }
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "connection failed with " + connectionResult.getErrorMessage()
                    + " and code: " + connectionResult.getErrorCode());
        }
        PendingIntent resolution =
                GoogleApiAvailability.getInstance().getErrorResolutionPendingIntent(mActivity,
                                                                                    connectionResult
                                                                                            .getErrorCode(),
                                                                                    RC_UPDATE_SERVICE);
        try {
            mActivity.startIntentSenderForResult(resolution.getIntentSender(),
                                                 RC_UPDATE_SERVICE,
                                                 null,
                                                 0,
                                                 0,
                                                 0);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
            mActivity.finish(RESULT_OK, mActivity.getIntent());
        }
    }


    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            mActivity.finish(RESULT_OK, mActivity.getIntent());
        } else {
            if (status.hasResolution()) {
                // Try to resolve the save request. This will prompt the user if
                // the credential is new.
                try {
                    status.startResolutionForResult(mActivity, RC_SAVE);
                } catch (IntentSender.SendIntentException e) {
                    // Could not resolve the request
                    Log.e(TAG, "STATUS: Failed to send resolution.", e);
                    mActivity.finish(RESULT_OK, mActivity.getIntent());
                }
            } else {
                mActivity.finish(RESULT_OK, mActivity.getIntent());
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == RC_SAVE) {
            if (resultCode == RESULT_OK) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "SAVE: OK");
                }
                mActivity.finish(RESULT_OK, new Intent());
            } else {
                Log.e(TAG, "SAVE: Canceled by user");
                mActivity.finish(RESULT_OK, new Intent());
            }
        } else if (requestCode == RC_UPDATE_SERVICE) {
            if (resultCode == RESULT_OK) {
                Credential credential = new Credential.Builder(mEmail).setPassword(mPassword)
                        .build();
                Auth.CredentialsApi
                        .save(mCredentialsApiClient, credential)
                        .setResultCallback(this);
            } else {
                Log.e(TAG, "SAVE: Canceled by user");
                mActivity.finish(RESULT_OK, new Intent());
            }
        }
    }

    /**
     * If SmartLock is enabled and Google Play Services is available, save the credentials.
     * Otherwise, finish the calling Activity with RESULT_OK.
     *
     * @param activity     the calling Activity.
     * @param parameters   calling Activity flow parameters.
     * @param firebaseUser Firebase user to save in Credential.
     * @param password     (optional) password for email credential.
     * @param provider     (optional) provider string for provider credential.
     */
    public static SmartLock saveCredentialOrFinish(AppCompatBase activity,
                                                   FlowParameters parameters,
                                                   FirebaseUser firebaseUser,
                                                   @Nullable String password,
                                                   @Nullable String provider) {
        // If SmartLock is disabled, finish the Activity
        if (!parameters.smartLockEnabled) {
            activity.finish(RESULT_OK, new Intent());
            return null;
        }

        // If Play Services is not available, finish the Activity
        if (!PlayServicesHelper.getInstance(activity).isPlayServicesAvailable()) {
            activity.finish(RESULT_OK, new Intent());
            return null;
        }

        if (!FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(parameters.appName)
                .isPlayServicesAvailable(activity)) {
            activity.finish(RESULT_OK, activity.getIntent());
            return null;
        }

        // Save credentials
        return new SmartLock(activity,
                             firebaseUser.getDisplayName(),
                             firebaseUser.getEmail(),
                             password,
                             provider,
                             firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl()
                                         .toString() : null);
    }
}
