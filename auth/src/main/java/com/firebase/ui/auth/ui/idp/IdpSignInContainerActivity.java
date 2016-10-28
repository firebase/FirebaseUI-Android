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

package com.firebase.ui.auth.ui.idp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.IdpProvider;
import com.firebase.ui.auth.provider.IdpProvider.IdpCallback;
import com.firebase.ui.auth.provider.TwitterProvider;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.AuthCredentialHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.util.SmartLock;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

public class IdpSignInContainerActivity extends AppCompatBase implements IdpCallback {
    private static final String TAG = "IDPSignInContainer";
    private static final int RC_WELCOME_BACK_IDP = 4;

    private IdpProvider mIdpProvider;
    private String mProvider;
    private String mEmail;
    @Nullable
    private SmartLock mSmartLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSmartLock = mActivityHelper.getSmartLockInstance(this, TAG);
        String provider = getIntent().getStringExtra(ExtraConstants.EXTRA_PROVIDER);
        String email = getIntent().getStringExtra(ExtraConstants.EXTRA_EMAIL);
        IdpConfig providerConfig = null;
        for (IdpConfig config : mActivityHelper.getFlowParams().providerInfo) {
            if (config.getProviderId().equalsIgnoreCase(provider)) {
                providerConfig = config;
                break;
            }
        }

        if (providerConfig == null) {
            // we don't have a provider to handle this
            finish(RESULT_CANCELED, new Intent());
            return;
        }

        if (provider.equalsIgnoreCase(FacebookAuthProvider.PROVIDER_ID)) {
            mIdpProvider = new FacebookProvider(this, providerConfig);
        } else if (provider.equalsIgnoreCase(GoogleAuthProvider.PROVIDER_ID)) {
            mIdpProvider = new GoogleProvider(this, providerConfig, email);
        } else if (provider.equalsIgnoreCase(TwitterAuthProvider.PROVIDER_ID)) {
            mIdpProvider = new TwitterProvider(this);
        }

        mIdpProvider.setAuthenticationCallback(this);
        mIdpProvider.startLogin(this);
    }

    @Override
    public void onSuccess(final IdpResponse response) {
        Intent data = new Intent();
        data.putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, response);
        AuthCredential credential = AuthCredentialHelper.getAuthCredential(response);
        final FirebaseAuth firebaseAuth = mActivityHelper.getFirebaseAuth();
        Task<AuthResult> authResultTask = firebaseAuth.signInWithCredential(credential);
        authResultTask
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Failure authenticating with credential"))
                .addOnCompleteListener(new CredentialSignInHandler(
                        IdpSignInContainerActivity.this,
                        mActivityHelper,
                        mSmartLock,
                        RC_WELCOME_BACK_IDP,
                        response));
    }

    @Override
    public void onFailure(Bundle extra) {
        finish(RESULT_CANCELED, new Intent());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_WELCOME_BACK_IDP) {
            finish(resultCode, data);
        } else {
            mIdpProvider.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            String provider,
            String email) {
        return ActivityHelper.createBaseIntent(
                context,
                IdpSignInContainerActivity.class,
                flowParams)
                .putExtra(ExtraConstants.EXTRA_PROVIDER, provider)
                .putExtra(ExtraConstants.EXTRA_EMAIL, email);
    }
}
