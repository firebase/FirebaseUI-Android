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

import android.content.Intent;
import android.support.annotation.NonNull;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.account_link.WelcomeBackIDPPrompt;
import com.firebase.ui.auth.ui.email.RegisterEmailActivity;
import com.firebase.ui.auth.ui.email.SignInActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ProviderQueryResult;

import java.util.Arrays;
import java.util.List;

public class AcquireEmailHelper {
    private static final String TAG = "AcquireEmailHelper";
    public static final int RC_REGISTER_ACCOUNT = 14;
    public static final int RC_WELCOME_BACK_IDP = 15;
    public static final int RC_SIGN_IN = 16;
    public static final List<Integer> REQUEST_CODES = Arrays.asList(
            RC_REGISTER_ACCOUNT,
            RC_WELCOME_BACK_IDP,
            RC_SIGN_IN
    );

    private ActivityHelper mActivityHelper;

    public AcquireEmailHelper(ActivityHelper activityHelper) {
        mActivityHelper = activityHelper;
    }

    public void checkAccountExists(final String email) {
        FirebaseAuth firebaseAuth = mActivityHelper.getFirebaseAuth();
        mActivityHelper.showLoadingDialog(R.string.progress_dialog_loading);
        if (email != null && !email.isEmpty()) {
            firebaseAuth
                    .fetchProvidersForEmail(email)
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error fetching providers for email"))
                    .addOnCompleteListener(
                            new OnCompleteListener<ProviderQueryResult>() {
                                @Override
                                public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                                    if (task.isSuccessful()) {
                                        startEmailHandler(email, task.getResult().getProviders());
                                    } else {
                                        mActivityHelper.dismissDialog();
                                    }
                                }
                            });
        }
    }

    private void startEmailHandler(String email, List<String> providers) {
        mActivityHelper.dismissDialog();
        if (providers == null || providers.isEmpty()) {
            // account doesn't exist yet
            Intent registerIntent = RegisterEmailActivity.createIntent(
                    mActivityHelper.getApplicationContext(),
                    mActivityHelper.getFlowParams(),
                    email);
            mActivityHelper.startActivityForResult(registerIntent, RC_REGISTER_ACCOUNT);
        } else {
            // account does exist
            for (String provider : providers) {
                if (provider.equalsIgnoreCase(EmailAuthProvider.PROVIDER_ID)) {
                    Intent signInIntent = SignInActivity.createIntent(
                            mActivityHelper.getApplicationContext(),
                            mActivityHelper.getFlowParams(),
                            email);
                    mActivityHelper.startActivityForResult(signInIntent, RC_SIGN_IN);
                    return;
                }

                Intent intent = WelcomeBackIDPPrompt.createIntent(
                        mActivityHelper.getApplicationContext(),
                        mActivityHelper.getFlowParams(),
                        provider,
                        null,
                        email);
                mActivityHelper.startActivityForResult(intent, RC_WELCOME_BACK_IDP);
                return;
            }

            Intent signInIntent = new Intent(
                    mActivityHelper.getApplicationContext(), SignInActivity.class);
            signInIntent.putExtra(ExtraConstants.EXTRA_EMAIL, email);
            mActivityHelper.startActivityForResult(signInIntent, RC_SIGN_IN);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_CODES.contains(requestCode)) {
            mActivityHelper.finish(resultCode, data);
        }
    }
}
