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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.provider.IDPProviderParcel;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.ui.idp.IDPSignInContainerActivity;
import com.firebase.ui.auth.util.CredentialsAPI;
import com.firebase.ui.auth.util.CredentialsApiHelper;
import com.firebase.ui.auth.util.EmailFlowUtil;
import com.firebase.ui.auth.util.PlayServicesHelper;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
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
    private static final int RC_PLAY_SERVICES = 6;

    private CredentialsAPI mCredentialsApi;
    private PlayServicesHelper mPlayServicesHelper;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        // Make Google Play Services available at the correct version, if possible
        mPlayServicesHelper = PlayServicesHelper.getInstance(this);
        boolean madeAvailable = mPlayServicesHelper
                .makePlayServicesAvailable(this, RC_PLAY_SERVICES,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                Log.w(TAG, "playServices:dialog.onCancel()");
                                finish(RESULT_CANCELED, new Intent());
                            }
                        });

        if (!madeAvailable) {
            Log.w(TAG, "playServices: could not make available.");
            finish(RESULT_CANCELED, new Intent());
            return;
        }

        mCredentialsApi = new CredentialsAPI(this, new CredentialsAPI.CallbackInterface() {
            @Override
            public void onAsyncTaskFinished() {
                onCredentialsApiConnected(mCredentialsApi, mActivityHelper);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCredentialsApi != null) {
            mCredentialsApi.onStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCredentialsApi != null) {
            mCredentialsApi.onStop();
        }
    }

    /**
     * Called when the Credentials API connects.
     */
    public void onCredentialsApiConnected(
            CredentialsAPI credentialsApi,
            ActivityHelper activityHelper) {

        String email = credentialsApi.getEmailFromCredential();
        String password = credentialsApi.getPasswordFromCredential();
        String accountType = credentialsApi.getAccountTypeFromCredential();

        FlowParameters flowParams = activityHelper.getFlowParams();

        if (flowParams.smartLockEnabled
                && mPlayServicesHelper.isPlayServicesAvailable()
                && credentialsApi.isCredentialsAvailable()) {

            // Attempt auto-sign in using SmartLock
            if (credentialsApi.isAutoSignInAvailable()) {
                credentialsApi.googleSilentSignIn();
                if (!TextUtils.isEmpty(password)) {
                    // Sign in with the email/password retrieved from SmartLock
                    signInWithEmailAndPassword(activityHelper, email, password);
                } else {
                    // log in with id/provider
                    redirectToIdpSignIn(email, accountType);
                }
            } else if (credentialsApi.isSignInResolutionNeeded()) {
                // resolve credential
                credentialsApi.resolveSavedEmails(this);
            } else {
                startAuthMethodChoice(activityHelper);
            }
        } else {
            startAuthMethodChoice(activityHelper);
        }
    }

    private void startAuthMethodChoice(ActivityHelper activityHelper) {
        List<IDPProviderParcel> providers = activityHelper.getFlowParams().providerInfo;

        // If the only provider is Email, immediately launch the email flow. Otherwise, launch
        // the auth method picker screen.
        if (providers.size() == 1
                && providers.get(0).getProviderType().equals(EmailAuthProvider.PROVIDER_ID)) {
            startActivityForResult(
                    EmailFlowUtil.createIntent(
                            this,
                            activityHelper.getFlowParams()),
                    RC_EMAIL_FLOW);
        } else {
            startActivityForResult(
                    AuthMethodPickerActivity.createIntent(
                            this,
                            activityHelper.getFlowParams()),
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
                signInWithEmailAndPassword(mActivityHelper, email, password);
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

        switch (requestCode) {
            case RC_CREDENTIALS_READ:
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
                } else if (resultCode == RESULT_CANCELED
                        || resultCode == CredentialsApi.ACTIVITY_RESULT_OTHER_ACCOUNT) {
                    // Smart lock selector cancelled, go to the AuthMethodPicker screen
                    startAuthMethodChoice(mActivityHelper);
                } else if (resultCode == RESULT_FIRST_USER) {
                    // TODO: (serikb) figure out flow
                }
                break;
            case RC_IDP_SIGNIN:
            case RC_AUTH_METHOD_PICKER:
            case RC_EMAIL_FLOW:
                finish(resultCode, new Intent());
                break;
            case RC_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    finish(resultCode, new Intent());
                }
                break;

        }
    }

    /**
     * Begin sign in process with email and password from a SmartLock credential.
     * On success, finish with {@link #RESULT_OK}.
     * On failure, delete the credential from SmartLock (if applicable) and then launch the
     * auth method picker flow.
     */
    private void signInWithEmailAndPassword(ActivityHelper helper, String email, String password) {
        helper.getFirebaseAuth()
                .signInWithEmailAndPassword(email, password)
                .addOnFailureListener(new TaskFailureLogger(
                        TAG, "Error signing in with email and password"))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        finish(RESULT_OK, new Intent());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof FirebaseAuthInvalidUserException) {
                            // In this case the credential saved in SmartLock was not
                            // a valid credential, we should delete it from SmartLock
                            // before continuing.
                            deleteCredentialAndRedirect();
                        } else {
                            startAuthMethodChoice(mActivityHelper);
                        }
                    }
                });
    }

    /**
     * Delete the last credential retrieved from SmartLock and then redirect to the
     * auth method choice flow.
     */
    private void deleteCredentialAndRedirect() {
        if (mCredentialsApi.getCredential() == null) {
            Log.w(TAG, "deleteCredentialAndRedirect: null credential");
            startAuthMethodChoice(mActivityHelper);
            return;
        }

        CredentialsApiHelper credentialsApiHelper = CredentialsApiHelper.getInstance(this);
        credentialsApiHelper.delete(mCredentialsApi.getCredential())
                .addOnCompleteListener(this, new OnCompleteListener<Status>() {
                    @Override
                    public void onComplete(@NonNull Task<Status> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "deleteCredential:failure", task.getException());
                        }
                        startAuthMethodChoice(mActivityHelper);
                    }
                });
    }

    protected void redirectToIdpSignIn(String email, String accountType) {
        Intent nextIntent;
        switch (accountType) {
            case IdentityProviders.GOOGLE:
                nextIntent = IDPSignInContainerActivity.createIntent(
                        this,
                        mActivityHelper.getFlowParams(),
                        GoogleAuthProvider.PROVIDER_ID,
                        email);
                break;
            case IdentityProviders.FACEBOOK:
                nextIntent = IDPSignInContainerActivity.createIntent(
                        this,
                        mActivityHelper.getFlowParams(),
                        FacebookAuthProvider.PROVIDER_ID,
                        email);
                break;
            default:
                Log.w(TAG, "unknown provider: " + accountType);
                nextIntent = AuthMethodPickerActivity.createIntent(
                        this,
                        mActivityHelper.getFlowParams());
        }
        this.startActivityForResult(nextIntent, RC_IDP_SIGNIN);
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams) {
        return ActivityHelper.createBaseIntent(context, ChooseAccountActivity.class, flowParams);
    }
}
