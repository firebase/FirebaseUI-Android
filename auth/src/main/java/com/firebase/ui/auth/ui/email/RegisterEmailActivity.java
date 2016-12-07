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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.accountlink.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.ui.accountlink.WelcomeBackPasswordPrompt;
import com.google.firebase.auth.EmailAuthProvider;

/**
 * Activity to control the entire email sign up flow. Plays host to {@link CheckEmailFragment}
 * and {@link RegisterEmailFragment} and triggers {@link WelcomeBackPasswordPrompt}.
 */
public class RegisterEmailActivity extends AppCompatBase
        implements CheckEmailFragment.CheckEmailListener {

    private static final int RC_SIGN_IN = 17;
    private static final int RC_WELCOME_BACK_IDP = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_email);

        // Get email from intent (can be null)
        String email = getIntent().getExtras().getString(ExtraConstants.EXTRA_EMAIL);

        // Start with check email
        CheckEmailFragment fragment = CheckEmailFragment.getInstance(
                mActivityHelper.getFlowParams(), email);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_register_email, fragment, CheckEmailFragment.TAG)
                .disallowAddToBackStack()
                .commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_SIGN_IN:
            case RC_WELCOME_BACK_IDP:
                finish(resultCode, new Intent());
        }
    }

    @Override
    public void onExistingEmailUser(@NonNull String email) {
        // Existing email user, direct them to sign in with their password.
        startActivityForResult(
                WelcomeBackPasswordPrompt.createIntent(
                        this,
                        mActivityHelper.getFlowParams(),
                        new IdpResponse(EmailAuthProvider.PROVIDER_ID, email)),
                RC_SIGN_IN);
    }

    @Override
    public void onExistingIdpUser(@NonNull String email, @NonNull String provider) {
        // Existing social user, direct them to sign in using their chosen provider.
        Intent intent = WelcomeBackIdpPrompt.createIntent(
                this,
                mActivityHelper.getFlowParams(),
                provider,
                null,
                email);
        mActivityHelper.startActivityForResult(intent,
                RC_WELCOME_BACK_IDP);
    }

    @Override
    public void onNewUser(@NonNull String email, @Nullable String name) {
        // New user, direct them to create an account with email/password.
        RegisterEmailFragment fragment = RegisterEmailFragment.getInstance(
                mActivityHelper.getFlowParams(),
                email,
                name);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_register_email, fragment, RegisterEmailFragment.TAG)
                .disallowAddToBackStack()
                .commit();
    }

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return createIntent(context, flowParams, null);
    }

    public static Intent createIntent(Context context, FlowParameters flowParams, String email) {
        return BaseHelper.createBaseIntent(context, RegisterEmailActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_EMAIL, email);
    }
}
