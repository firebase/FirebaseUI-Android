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

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentTransaction;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.idp.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.util.ExtraConstants;

/**
 * Activity to control the entire email sign up flow. Plays host to {@link CheckEmailFragment} and
 * {@link RegisterEmailFragment} and triggers {@link WelcomeBackPasswordPrompt} and {@link
 * WelcomeBackIdpPrompt}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailActivity extends AppCompatBase implements
        CheckEmailFragment.CheckEmailListener {

    private static final int RC_WELCOME_BACK_IDP = 15;
    private static final int RC_SIGN_IN = 16;

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return createIntent(context, flowParams, null);
    }

    public static Intent createIntent(Context context, FlowParameters flowParams, String email) {
        return HelperActivityBase.createBaseIntent(context, EmailActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_EMAIL, email);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_activity_register_email);

        getSignInHandler().getSignInLiveData().observe(this, new Observer<IdpResponse>() {
            @Override
            public void onChanged(@Nullable IdpResponse response) {
                if (response.isSuccessful()) {
                    finish(Activity.RESULT_OK, response.toIntent());
                }
            }
        });

        if (savedInstanceState != null) {
            return;
        }

        // Get email from intent (can be null)
        String email = getIntent().getExtras().getString(ExtraConstants.EXTRA_EMAIL);

        // Start with check email
        CheckEmailFragment fragment = CheckEmailFragment.newInstance(email);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_register_email, fragment, CheckEmailFragment.TAG)
                .disallowAddToBackStack()
                .commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ExtraConstants.HAS_EXISTING_INSTANCE, true);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_SIGN_IN:
            case RC_WELCOME_BACK_IDP:
                finish(resultCode, data);
        }
    }

    @Override
    public void onExistingEmailUser(User user) {
        // Existing email user, direct them to sign in with their password.
        startActivityForResult(
                WelcomeBackPasswordPrompt.createIntent(this, getFlowHolder().getParams(), user),
                RC_SIGN_IN);

        setSlideAnimation();
    }

    @Override
    public void onExistingIdpUser(User user) {
        // Existing social user, direct them to sign in using their chosen provider.
        startActivityForResult(
                WelcomeBackIdpPrompt.createIntent(this, getFlowHolder().getParams(), user),
                RC_WELCOME_BACK_IDP);
        setSlideAnimation();
    }

    @Override
    public void onNewUser(User user) {
        // New user, direct them to create an account with email/password
        // if account creation is enabled in SignInIntentBuilder

        TextInputLayout emailLayout = findViewById(R.id.email_layout);

        if (getFlowHolder().getParams().allowNewEmailAccounts) {
            RegisterEmailFragment fragment = RegisterEmailFragment.newInstance(user);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_register_email, fragment, RegisterEmailFragment.TAG);

            if (emailLayout != null) ft.addSharedElement(emailLayout, "email_field");

            ft.disallowAddToBackStack().commit();
        } else {
            emailLayout.setError(getString(R.string.fui_error_email_does_not_exist));
        }
    }

    private void setSlideAnimation() {
        // Make the next activity slide in
        overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
    }
}
