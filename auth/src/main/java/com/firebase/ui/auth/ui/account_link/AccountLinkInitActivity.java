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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.util.FirebaseAuthWrapper;
import com.firebase.ui.auth.util.FirebaseAuthWrapperFactory;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ProviderQueryResult;

import java.util.Arrays;
import java.util.List;

public class AccountLinkInitActivity extends AppCompatBase {
    protected FirebaseAuthWrapper mApiWrapper;
    private static final int RC_SAVE_CREDENTIALS = 3;
    private static final int RC_WELCOME_BACK_IDP_PROMPT = 4;
    private static final int RC_WELCOME_BACK_PASSWORD_PROMPT = 5;
    private static final List<Integer> REQUEST_CODES = Arrays.asList(
            RC_SAVE_CREDENTIALS,
            RC_WELCOME_BACK_IDP_PROMPT,
            RC_WELCOME_BACK_PASSWORD_PROMPT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityHelper.showLoadingDialog(R.string.progress_dialog_loading);
        mApiWrapper = FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(
                mActivityHelper.getAppName());
        String email = getIntent().getStringExtra(ExtraConstants.EXTRA_EMAIL);
        String password = getIntent().getStringExtra(ExtraConstants.EXTRA_PASSWORD);
        String provider = getIntent().getStringExtra(ExtraConstants.EXTRA_PROVIDER);
        next(email, password, provider);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_CODES.contains(requestCode)) {
            finish(resultCode, data);
        }
    }

    void next(final String email, final String password, final String provider) {
        if (email == null) {
            finish(RESULT_OK, new Intent());
            return;
        }
        FirebaseAuth firebaseAuth = mActivityHelper.getFirebaseAuth();
        Task<ProviderQueryResult> providerQueryResultTask
                = firebaseAuth.fetchProvidersForEmail(email);
        providerQueryResultTask.addOnCompleteListener(
                new OnCompleteListener<ProviderQueryResult>() {
            @Override
            public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                mActivityHelper.dismissDialog();
                List<String> providers = task.getResult().getProviders();
                if (providers.size() == 0) {
                    // new account for this email
                    startActivityForResult(SaveCredentialsActivity.createIntent(
                            getApplicationContext(),
                            null,
                            email,
                            password,
                            provider,
                            null,
                            mActivityHelper.getAppName()), RC_SAVE_CREDENTIALS);
                } else if (providers.size() == 1) {
                    if (providers.get(0).equals(provider)) {
                        // existing account but has this IDP linked
                        startActivityForResult(SaveCredentialsActivity.createIntent(
                                AccountLinkInitActivity.this,
                                null,
                                email,
                                password,
                                provider,
                                null,
                                mActivityHelper.getAppName()),
                                RC_SAVE_CREDENTIALS);
                    } else {
                        if (providers.get(0).equals(EmailAuthProvider.PROVIDER_ID)) {
                            startActivityForResult(WelcomeBackPasswordPrompt.createIntent(
                                    getApplicationContext(),
                                    mActivityHelper.flowParams,
                                    email),
                                    RC_WELCOME_BACK_PASSWORD_PROMPT);
                        } else {
                            // existing account but has a different IDP linked
                            startActivityForResult(WelcomeBackIDPPrompt.createIntent(
                                    getApplicationContext(),
                                    mActivityHelper.flowParams,
                                    provider,
                                    email),
                                RC_WELCOME_BACK_IDP_PROMPT
                            );
                        }
                    }
                } else {
                    // more than one providers
                    startActivityForResult(WelcomeBackIDPPrompt.createIntent(
                            getApplicationContext(),
                            mActivityHelper.flowParams,
                            provider,
                            email),
                            RC_WELCOME_BACK_IDP_PROMPT);
                }
            }
        });
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            String email,
            String password,
            String provider) {
        return ActivityHelper.createBaseIntent(context, AccountLinkInitActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_EMAIL, email)
                .putExtra(ExtraConstants.EXTRA_PASSWORD, password)
                .putExtra(ExtraConstants.EXTRA_PROVIDER, provider);
    }
}
