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

package com.firebase.ui.auth.ui.email;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.ui.NoControllerBaseActivity;
import com.firebase.ui.auth.ui.account_link.WelcomeBackIDPPrompt;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ProviderQueryResult;

import java.util.Arrays;
import java.util.List;

public abstract class AcquireEmailActivity extends NoControllerBaseActivity {
    protected static final int RC_REGISTER_ACCOUNT = 14;
    protected static final int RC_WELCOME_BACK_IDP = 15;
    protected static final int RC_SIGN_IN = 16;
    private static final List<Integer> REQUEST_CODES = Arrays.asList(
            RC_REGISTER_ACCOUNT,
            RC_WELCOME_BACK_IDP,
            RC_SIGN_IN
    );

    protected void checkAccountExists(final String email) {
        FirebaseAuth firebaseAuth = getFirebaseAuth();
        showLoadingDialog(R.string.progress_dialog_loading);
        if (email != null && !email.isEmpty()) {
            firebaseAuth.fetchProvidersForEmail(email).addOnCompleteListener(
                    new OnCompleteListener<ProviderQueryResult>() {
                        @Override
                        public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                            startEmailHandler(email, task.getResult().getProviders());
                        }
                    }
            );
        }
    }

    private void startEmailHandler(String email, List<String> providers) {
        dismissDialog();
        if (providers == null || providers.isEmpty()) {
            // account doesn't exist yet
            Intent registerIntent = RegisterEmailActivity.createIntent(
                    AcquireEmailActivity.this,
                    email,
                    mTermsOfServiceUrl,
                    mAppName
            );
            startActivityForResult(registerIntent, RC_REGISTER_ACCOUNT);
            return;
        } else {
            // account does exist
            for (String provider: providers) {
                if (provider.equalsIgnoreCase(EmailAuthProvider.PROVIDER_ID)) {
                    Intent signInIntent = SignInActivity.createIntent(
                            this,
                            mAppName,
                            email,
                            mProviderParcels
                    );
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                    return;
                }
                Intent intent = WelcomeBackIDPPrompt.createIntent(
                        AcquireEmailActivity.this,
                        provider,
                        mProviderParcels,
                        mAppName,
                        email);
                startActivityForResult(intent, RC_WELCOME_BACK_IDP);
                return;
            }

            Intent signInIntent = new Intent(
                    AcquireEmailActivity.this, SignInActivity.class);
            signInIntent.putExtra(ControllerConstants.EXTRA_EMAIL, email);
            startActivityForResult(signInIntent, RC_SIGN_IN);
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODES.contains(requestCode)) {
            finish(resultCode, new Intent());
        }
    }
}
