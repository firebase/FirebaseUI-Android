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

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt;
import com.firebase.ui.auth.util.accountlink.AccountLinker;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.ProviderQueryResult;

import java.util.List;

/**
 * Failure listener passed to calls to {@link FirebaseAuth#signInWithCredential(AuthCredential)}.
 *
 * On collisions, starts the "Welcome Back" flow for the appropriate IDP.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CredentialSignInHandler implements OnFailureListener {
    private static final String TAG = "CredentialSignInHandler";

    private HelperActivityBase mActivity;
    private IdpResponse mResponse;
    private int mAccountLinkRequestCode;

    public CredentialSignInHandler(
            HelperActivityBase activity,
            int accountLinkRequestCode,
            IdpResponse response) {
        mActivity = activity;
        mResponse = response;
        mAccountLinkRequestCode = accountLinkRequestCode;
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            Task<ProviderQueryResult> fetchTask;
            String email = mResponse.getEmail();
            if (TextUtils.isEmpty(email)) {
                fetchTask = Tasks.forException(new NullPointerException("Email cannot be empty"));
            } else {
                fetchTask = mActivity.getAuthHelper().getFirebaseAuth()
                        .fetchProvidersForEmail(email);
            }

            fetchTask
                    .addOnSuccessListener(new StartWelcomeBackFlow())
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Intent intent = IdpResponse.getErrorIntent(e);
                            mActivity.finish(Activity.RESULT_CANCELED, intent);
                        }
                    })
                    .addOnFailureListener(new TaskFailureLogger(TAG,
                                                                "Error fetching providers for email"));
            return;
        } else {
            Log.e(TAG,
                  "Unexpected exception when signing in with credential "
                          + mResponse.getProviderType()
                          + " unsuccessful. Visit https://console.firebase.google.com to enable it.",
                  e);
        }

        mActivity.getDialogHolder().dismissDialog();
    }

    private class StartWelcomeBackFlow implements OnSuccessListener<ProviderQueryResult> {
        @Override
        public void onSuccess(ProviderQueryResult result) {
            List<String> providers = result.getProviders();
            AuthCredential credential = ProviderUtils.getAuthCredential(mResponse);
            if (mActivity.getAuthHelper().canLinkAccounts()
                    && credential != null
                    && providers != null && providers.contains(credential.getProvider())) {
                // We don't want to show the welcome back dialog since the user selected
                // an existing account and we can just link the two accounts without knowing
                // prevCredential.
                AccountLinker.linkWithCurrentUser(mActivity, mResponse, credential)
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult result) {
                                mActivity.finish(Activity.RESULT_OK, mResponse.toIntent());
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mActivity.finish(Activity.RESULT_CANCELED,
                                        IdpResponse.getErrorCodeIntent(ErrorCodes.UNKNOWN_ERROR));
                            }
                        });
                return;
            }
            mActivity.getDialogHolder().dismissDialog();

            @AuthUI.SupportedProvider String provider = ProviderUtils.getLastUsedProvider(result);
            if (provider == null) {
                throw new IllegalStateException(
                        "No provider even though we received a FirebaseAuthUserCollisionException");
            } else if (provider.equals(EmailAuthProvider.PROVIDER_ID)) {
                // Start email welcome back flow
                mActivity.startActivityForResult(
                        WelcomeBackPasswordPrompt.createIntent(
                                mActivity,
                                mActivity.getFlowParams(),
                                mResponse),
                        mAccountLinkRequestCode);
            } else {
                // Start Idp welcome back flow
                mActivity.startActivityForResult(
                        WelcomeBackIdpPrompt.createIntent(
                                mActivity,
                                mActivity.getFlowParams(),
                                new User.Builder(provider, mResponse.getEmail()).build(),
                                mResponse),
                        mAccountLinkRequestCode);
            }
        }
    }
}
