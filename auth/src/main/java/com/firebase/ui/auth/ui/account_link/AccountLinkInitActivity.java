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
import com.firebase.ui.auth.api.FirebaseAuthWrapper;
import com.firebase.ui.auth.api.FirebaseAuthWrapperFactory;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.ui.NoControllerBaseActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ProviderQueryResult;

import java.util.List;

public class AccountLinkInitActivity extends NoControllerBaseActivity {
    FirebaseAuthWrapper mApiWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLoadingDialog(getResources().getString(R.string.progress_dialog_loading));
        mApiWrapper = FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(mAppName);
        String email = getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);
        String password = getIntent().getStringExtra(ControllerConstants.EXTRA_PASSWORD);
        String provider = getIntent().getStringExtra(ControllerConstants.EXTRA_PROVIDER);
        next(email, password, provider);
    }

    public static Intent createStartIntent(Context context, String appName, String id,
                                           String provider) {
        return new Intent(context, AccountLinkInitActivity.class)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
                .putExtra(ControllerConstants.EXTRA_EMAIL, id)
                .putExtra(ControllerConstants.EXTRA_PROVIDER, provider);
    }

    void next(final String email, final String password, final String provider) {
        if (email == null) {
            finish();
        }
        FirebaseAuth firebaseAuth = getFirebaseAuth();
        Task<ProviderQueryResult> providerQueryResultTask = firebaseAuth.fetchProvidersForEmail(email);
        providerQueryResultTask.addOnCompleteListener(new OnCompleteListener<ProviderQueryResult>() {
            @Override
            public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                List<String> providers = task.getResult().getProviders();
                if (providers.size() == 0) {
                    // new account for this email
                    startActivity(SaveCredentialsActivity.createIntent(
                            getApplicationContext(),
                            null,
                            email,
                            password,
                            provider,
                            null,
                            mAppName));
                    finish();
                } else if (providers.size() == 1) {
                    if (providers.get(0).equals(provider)) {
                        // existing account but has this IDP linked
                        startActivity(new Intent(getApplicationContext(), SaveCredentialsActivity.class)
                                .putExtra(ControllerConstants.EXTRA_EMAIL, email)
                                .putExtra(ControllerConstants.EXTRA_PROVIDER, provider)
                                .putExtra(ControllerConstants.EXTRA_PASSWORD, password));
                        finish();
                    } else {
                        if (providers.get(0).equals(EmailAuthProvider.PROVIDER_ID)) {
                            startActivity(
                                    new Intent(getApplicationContext(), WelcomeBackPasswordPrompt.class)
                                            .putExtra(ControllerConstants.EXTRA_EMAIL, email)
                                            .putExtra(ControllerConstants.EXTRA_APP_NAME, mAppName));
                            finish();
                        } else {
                            // existing account but has a different IDP linked
                            startActivity(new Intent(getApplicationContext(), WelcomeBackIDPPrompt.class)
                                    .putExtra(ControllerConstants.EXTRA_EMAIL, email)
                                    .putExtra(ControllerConstants.EXTRA_PROVIDER, provider)
                            );
                        }
                    }
                } else {
                    // more than one providers
                    startActivity(new Intent(getApplicationContext(), WelcomeBackIDPPrompt.class)
                            .putExtra(ControllerConstants.EXTRA_EMAIL, email));
                    finish();
                }
            }
        });
    }
}
