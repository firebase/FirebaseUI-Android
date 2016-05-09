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

package com.firebase.ui.auth.ui.account_link;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.api.FirebaseAuthWrapperFactory;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.ui.NoControllerBaseActivity;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;

public class SaveCredentialsActivity extends NoControllerBaseActivity
        implements GoogleApiClient.ConnectionCallbacks, ResultCallback<Status>,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "CredentialsSaveBase";
    private static final int RC_SAVE = 100;
    private static final int RC_UPDATE_SERVICE = 28;

    private String mName;
    private String mEmail;
    private String mPassword;
    private String mProvider;
    private String mProfilePictureUri;
    private GoogleApiClient mCredentialsApiClient;


    public static Intent createIntent(
            Context context,
            String name,
            String email,
            String password,
            String provider,
            String profilePictureUri,
            String appName) {
        return new Intent().setClass(context, SaveCredentialsActivity.class)
                .putExtra(ControllerConstants.EXTRA_NAME, name)
                .putExtra(ControllerConstants.EXTRA_EMAIL, email)
                .putExtra(ControllerConstants.EXTRA_PASSWORD, password)
                .putExtra(ControllerConstants.EXTRA_PROVIDER, provider)
                .putExtra(ControllerConstants.EXTRA_PROFILE_PICTURE_URI, profilePictureUri)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save_credentials_layout);
        if (!FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(mAppName)
                .isPlayServicesAvailable(this)) {
            finish(RESULT_FIRST_USER, getIntent());
        }
        mName = getIntent().getStringExtra(ControllerConstants.EXTRA_NAME);
        mEmail = getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);
        mPassword = getIntent().getStringExtra(ControllerConstants.EXTRA_PASSWORD);
        mProvider = getIntent().getStringExtra(ControllerConstants.EXTRA_PROVIDER);
        mProfilePictureUri = getIntent()
                .getStringExtra(ControllerConstants.EXTRA_PROFILE_PICTURE_URI);

        mCredentialsApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
        mCredentialsApiClient.connect();
    }


    @Override
    protected void onStart() {
        super.onStart();
        mCredentialsApiClient.connect();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mEmail == null) {
            Log.e(TAG, "Unable to save null credential!");
            finish(RESULT_FIRST_USER, getIntent());
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
        Auth.CredentialsApi.save(mCredentialsApiClient, builder.build()).setResultCallback(this);
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
                GoogleApiAvailability.getInstance().getErrorResolutionPendingIntent(this,
                        connectionResult.getErrorCode(), RC_UPDATE_SERVICE);
        try {
            startIntentSenderForResult(resolution.getIntentSender(), RC_UPDATE_SERVICE, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
            finish(RESULT_FIRST_USER, getIntent());
        }
    }


    @Override
    public void onResult(@NonNull Status status) {

        if (status.isSuccess()) {
            Toast.makeText(getApplicationContext(), "Credential saved", Toast.LENGTH_SHORT).show();
            finish(RESULT_OK, getIntent());
        } else {
            if (status.hasResolution()) {
                // Try to resolve the save request. This will prompt the user if
                // the credential is new.
                try {
                    status.startResolutionForResult(this, RC_SAVE);
                } catch (IntentSender.SendIntentException e) {
                    // Could not resolve the request
                    Log.e(TAG, "STATUS: Failed to send resolution.", e);
                    finish(RESULT_FIRST_USER, getIntent());
                }
            } else {
                finish(RESULT_FIRST_USER, getIntent());
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
                finish(RESULT_OK, getIntent());
            } else {
                Log.e(TAG, "SAVE: Canceled by user");
                finish(RESULT_FIRST_USER, getIntent());
            }
        } else if (requestCode == RC_UPDATE_SERVICE) {
            if (resultCode == RESULT_OK) {
                Credential credential = new Credential.Builder(mEmail).setPassword(mPassword).build();
                Auth.CredentialsApi.save(mCredentialsApiClient, credential).setResultCallback(this);
            } else {
                Log.e(TAG, "SAVE: Canceled by user");
                finish(RESULT_FIRST_USER, getIntent());
            }
        }
    }}
