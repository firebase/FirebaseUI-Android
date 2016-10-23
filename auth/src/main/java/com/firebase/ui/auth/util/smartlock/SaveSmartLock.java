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

package com.firebase.ui.auth.util.smartlock;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.util.FirebaseAuthWrapperFactory;
import com.firebase.ui.auth.util.PlayServicesHelper;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseUser;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;

public class SaveSmartLock extends SmartLock<Status> {
    private static final String TAG = "SaveSmartLock";
    private static final int RC_SAVE = 100;
    private static final int RC_UPDATE_SERVICE = 28;

    private GoogleApiClient mGoogleApiClient;
    private String mName;
    private String mEmail;
    private String mPassword;
    private String mProvider;
    private String mProfilePictureUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!mHelper.getFlowParams().smartLockEnabled
                || !PlayServicesHelper.getInstance(getActivity()).isPlayServicesAvailable()
                || !FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(mHelper.getFlowParams().appName)
                .isPlayServicesAvailable(getActivity())) {
            finish(RESULT_CANCELED);
            return;
        }

        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (TextUtils.isEmpty(mEmail)) {
            Log.e(TAG, "Unable to save null credential!");
            finish(RESULT_CANCELED);
            return;
        }

        Credential.Builder builder = new Credential.Builder(mEmail);
        builder.setPassword(mPassword);
        if (mPassword == null) {
            // only password OR provider can be set, not both
            if (mProvider != null) {
                String translatedProvider = SmartLock.providerIdToAccountType(mProvider);

                if (translatedProvider != null) {
                    builder.setAccountType(translatedProvider);
                } else {
                    Log.e(TAG, "Unable to save null credential!");
                    finish(RESULT_CANCELED);
                    return;
                }
            }
        }

        if (mName != null) {
            builder.setName(mName);
        }

        if (mProfilePictureUri != null) {
            builder.setProfilePictureUri(Uri.parse(mProfilePictureUri));
        }

        mHelper.getCredentialsApi()
                .save(mGoogleApiClient, builder.build())
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
        Toast.makeText(getActivity(), "An error has occurred.", Toast.LENGTH_SHORT).show();

        PendingIntent resolution =
                GoogleApiAvailability
                        .getInstance()
                        .getErrorResolutionPendingIntent(getActivity(),
                                                         connectionResult.getErrorCode(),
                                                         RC_UPDATE_SERVICE);
        try {
            startIntentSenderForResult(resolution.getIntentSender(),
                                       RC_UPDATE_SERVICE,
                                       null,
                                       0,
                                       0,
                                       0,
                                       null);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "STATUS: Failed to send resolution.", e);
            finish(RESULT_CANCELED);
        }
    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            finish(RESULT_OK);
        } else {
            if (status.hasResolution()) {
                // Try to resolve the save request. This will prompt the user if
                // the credential is new.
                try {
                    startIntentSenderForResult(status.getResolution().getIntentSender(),
                                               RC_SAVE,
                                               null,
                                               0,
                                               0,
                                               0,
                                               null);
                } catch (IntentSender.SendIntentException e) {
                    // Could not resolve the request
                    Log.e(TAG, "STATUS: Failed to send resolution.", e);
                    finish(RESULT_CANCELED);
                }
            } else {
                finish(RESULT_CANCELED);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SAVE) {
            if (resultCode == RESULT_OK) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "SAVE: OK");
                }
                finish(RESULT_OK);
            } else {
                Log.e(TAG, "SAVE: Canceled by user");
                finish(RESULT_FIRST_USER);
            }
        } else if (requestCode == RC_UPDATE_SERVICE) {
            if (resultCode == RESULT_OK) {
                Credential credential = new Credential.Builder(mEmail).setPassword(mPassword)
                        .build();
                mHelper.getCredentialsApi()
                        .save(mGoogleApiClient, credential)
                        .setResultCallback(this);
            } else {
                Log.e(TAG, "SAVE: Canceled by user");
                finish(RESULT_FIRST_USER);
            }
        }
    }

    private void finish(int resultCode) {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        ((AppCompatBase) getActivity()).finish(RESULT_OK, getActivity().getIntent());
    }

    /**
     * If SmartLock is enabled and Google Play Services is available, save the credentials.
     * Otherwise, finish the calling Activity with RESULT_OK.
     *
     * @param firebaseUser Firebase user to save in Credential.
     * @param password     (optional) password for email credential.
     * @param provider     (optional) provider string for provider credential.
     */
    public void saveCredentialsOrFinish(FirebaseUser firebaseUser,
                                        @Nullable String password,
                                        @Nullable String provider) {
        mName = firebaseUser.getDisplayName();
        mEmail = firebaseUser.getEmail();
        mPassword = password;
        mProvider = provider;
        mProfilePictureUri = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl()
                .toString() : null;
    }

    public static SaveSmartLock getInstance(FragmentActivity activity,
                                            FlowParameters parameters,
                                            String tag) {
        SaveSmartLock result;

        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        Fragment fragment = fm.findFragmentByTag(tag);
        if (fragment == null || !(fragment instanceof SaveSmartLock)) {
            result = new SaveSmartLock();

            Bundle bundle = new Bundle();
            bundle.putParcelable(ExtraConstants.EXTRA_FLOW_PARAMS, parameters);
            result.setArguments(bundle);

            ft.add(result, tag).disallowAddToBackStack().commit();
        } else {
            result = (SaveSmartLock) fragment;
        }

        return result;
    }
}
