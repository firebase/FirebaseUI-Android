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

package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.provider.IDPProviderParcel;
import com.firebase.ui.auth.ui.email.EmailHintContainerActivity;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.ui.idp.IDPSignInContainerActivity;
import com.firebase.ui.auth.util.CredentialsAPI;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.List;

/**
 * Attempts to acquire a credential from Smart Lock for Passwords to sign in
 * an existing account. If this succeeds, an attempt is made to sign the user in
 * with this credential. If it does not, the
 * {@link AuthMethodPickerActivity authentication method picker activity}
 * is started, unless only email is supported, in which case the
 * {@link com.firebase.ui.auth.ui.email.SignInNoPasswordActivity email sign-in flow}
 * is started.
 */
public class ChooseAccountActivity extends ActivityBase {
    private static final String TAG = "ChooseAccountActivity";
    private static final int RC_CREDENTIALS_READ = 2;
    private static final int RC_IDP_SIGNIN = 3;
    private static final int RC_AUTH_METHOD_PICKER = 4;
    private static final int RC_EMAIL_FLOW = 5;

    protected CredentialsAPI mCredentialsApi;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        mCredentialsApi = new CredentialsAPI(this, new CredentialsAPI.CallbackInterface() {
            @Override
            public void onAsyncTaskFinished() {
                onCredentialsApiConnected();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCredentialsApi.isGoogleApiClient()) {
            mCredentialsApi.getGoogleApiClient().connect();
        }
    }

    @Override
    protected void onStop() {
        if (mCredentialsApi.isGoogleApiClient()
                && mCredentialsApi.getGoogleApiClient().isConnected()) {
            mCredentialsApi.getGoogleApiClient().disconnect();
        }
        super.onStop();
    }

    public void onCredentialsApiConnected() {
        // called back when the CredentialsAPI connects
        String email = mCredentialsApi.getEmailFromCredential();
        String password = mCredentialsApi.getPasswordFromCredential();
        String accountType = mCredentialsApi.getAccountTypeFromCredential();
        if (mCredentialsApi.isPlayServicesAvailable()
                && mCredentialsApi.isCredentialsAvailable()) {
            if (mCredentialsApi.isAutoSignInAvailable()) {
                mCredentialsApi.googleSilentSignIn();
                // TODO: (serikb) authenticate Firebase user and continue to application
                if (password != null && !password.isEmpty()) {
                    // login with username/password
                    mActivityHelper.getFirebaseAuth().signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        finish(Activity.RESULT_OK, new Intent());
                                    } else {
                                        Log.e(TAG, "Unsuccessful sign in with email and password",
                                                task.getException());
                                    }
                                }
                            });
                } else {
                    // log in with id/provider
                    redirectToIdpSignIn(email, accountType);
                }
            } else if (mCredentialsApi.isSignInResolutionNeeded()) {
                // resolve credential
                mCredentialsApi.resolveSavedEmails(this);
            } else {
                startAuthMethodChoice();
            }
        } else {
            startAuthMethodChoice();
        }
    }

    private void startAuthMethodChoice() {
        List<IDPProviderParcel> providers = mActivityHelper.flowParams.providerInfo;
        if (providers.size() == 1
                && providers.get(0).getProviderType().equals(EmailAuthProvider.PROVIDER_ID)) {

            startActivityForResult(
                    EmailHintContainerActivity.createIntent(
                            this,
                            mActivityHelper.flowParams),
                    RC_EMAIL_FLOW);
        } else {
            startActivityForResult(
                    AuthMethodPickerActivity.createIntent(
                            this,
                            mActivityHelper.flowParams),
                    RC_AUTH_METHOD_PICKER);
        }
    }

    private void logInWithCredential(
            final String email,
            final String password,
            final String accountType) {
        if (email != null
                && mCredentialsApi.isCredentialsAvailable()
                && !mCredentialsApi.isSignInResolutionNeeded()) {
            if (password != null && !password.isEmpty()) {
                // email/password combination
                mActivityHelper.getFirebaseAuth().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    finish(RESULT_OK, new Intent());
                                } else {
                                    // email/password auth failed, go to the
                                    // AuthMethodPickerActivity
                                    startActivityForResult(
                                            AuthMethodPickerActivity.createIntent(
                                                ChooseAccountActivity.this,
                                                mActivityHelper.flowParams),
                                            RC_AUTH_METHOD_PICKER);
                                }
                            }
                        });
            } else {
                // identifier/provider combination
                redirectToIdpSignIn(email, accountType);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
        }

        if (requestCode == RC_CREDENTIALS_READ) {
            if (resultCode == RESULT_OK) {
                // credential selected from SmartLock, log in with that credential
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                mCredentialsApi.handleCredential(credential);
                mCredentialsApi.resolveSignIn();
                logInWithCredential(
                        mCredentialsApi.getEmailFromCredential(),
                        mCredentialsApi.getPasswordFromCredential(),
                        mCredentialsApi.getAccountTypeFromCredential()
                );
            } else if (resultCode == RESULT_CANCELED) {
                // Smart lock selector cancelled, go to the AuthMethodPicker screen
                startAuthMethodChoice();
            } else if (resultCode == RESULT_FIRST_USER) {
                // TODO: (serikb) figure out flow
            }
        } else if (requestCode == RC_IDP_SIGNIN) {
            finish(resultCode, new Intent());
        } else if (requestCode == RC_AUTH_METHOD_PICKER) {
            finish(resultCode, new Intent());
        } else if (requestCode == RC_EMAIL_FLOW) {
            finish(resultCode, new Intent());
        }
    }

    protected void redirectToIdpSignIn(String email, String accountType) {
        Intent nextIntent;
        switch (accountType) {
            case IdentityProviders.GOOGLE:
                nextIntent = IDPSignInContainerActivity.createIntent(
                        this,
                        mActivityHelper.flowParams,
                        GoogleAuthProvider.PROVIDER_ID,
                        email);
                break;
            case IdentityProviders.FACEBOOK:
                nextIntent = IDPSignInContainerActivity.createIntent(
                        this,
                        mActivityHelper.flowParams,
                        FacebookAuthProvider.PROVIDER_ID,
                        email);
                break;
            default:
                Log.w(TAG, "unknown provider: " + accountType);
                nextIntent = AuthMethodPickerActivity.createIntent(
                        this,
                        mActivityHelper.flowParams);
        }
        this.startActivityForResult(nextIntent, RC_IDP_SIGNIN);
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams) {
        return ActivityHelper.createBaseIntent(context, ChooseAccountActivity.class, flowParams);
    }
}
