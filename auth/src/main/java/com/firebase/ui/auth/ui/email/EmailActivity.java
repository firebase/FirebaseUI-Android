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

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.idp.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.EmailLinkPersistenceManager;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.EmailAuthProvider;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentTransaction;

import static com.firebase.ui.auth.AuthUI.EMAIL_LINK_PROVIDER;

/**
 * Activity to control the entire email sign up flow. Plays host to {@link CheckEmailFragment} and
 * {@link RegisterEmailFragment} and triggers {@link WelcomeBackPasswordPrompt} and {@link
 * WelcomeBackIdpPrompt}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailActivity extends AppCompatBase implements CheckEmailFragment.CheckEmailListener,
        RegisterEmailFragment.AnonymousUpgradeListener, EmailLinkFragment
                .TroubleSigningInListener, TroubleSigningInFragment.ResendEmailListener {

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return createBaseIntent(context, EmailActivity.class, flowParams);
    }

    public static Intent createIntent(Context context, FlowParameters flowParams, String email) {
        return createBaseIntent(context, EmailActivity.class, flowParams)
                .putExtra(ExtraConstants.EMAIL, email);
    }

    public static Intent createIntentForLinking(Context context, FlowParameters flowParams,
                                                IdpResponse responseForLinking) {
        return createIntent(context, flowParams, responseForLinking.getEmail())
                .putExtra(ExtraConstants.IDP_RESPONSE, responseForLinking);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_activity_register_email);

        if (savedInstanceState != null) {
            return;
        }

        // Get email from intent (can be null)
        String email = getIntent().getExtras().getString(ExtraConstants.EMAIL);

        IdpResponse responseForLinking = getIntent().getExtras().getParcelable(ExtraConstants
                .IDP_RESPONSE);
        if (email != null && responseForLinking != null) {
            // got here from WelcomeBackEmailLinkPrompt
            AuthUI.IdpConfig emailConfig = ProviderUtils.getConfigFromIdpsOrThrow(
                    getFlowParams().providers, EMAIL_LINK_PROVIDER);
            ActionCodeSettings actionCodeSettings = emailConfig.getParams().getParcelable
                    (ExtraConstants.ACTION_CODE_SETTINGS);

            EmailLinkPersistenceManager.getInstance().saveIdpResponseForLinking(getApplication(),
                    responseForLinking);

            boolean forceSameDevice =
                    emailConfig.getParams().getBoolean(ExtraConstants.FORCE_SAME_DEVICE);
            EmailLinkFragment fragment = EmailLinkFragment.newInstance(email, actionCodeSettings,
                    responseForLinking, forceSameDevice);
            switchFragment(fragment, R.id.fragment_register_email, EmailLinkFragment.TAG);
        } else {
            AuthUI.IdpConfig emailConfig = ProviderUtils.getConfigFromIdps(
                    getFlowParams().providers, EmailAuthProvider.PROVIDER_ID);

            if (emailConfig != null) {
                email =  emailConfig.getParams().getString(ExtraConstants.DEFAULT_EMAIL);;
            }
            // Start with check email
            CheckEmailFragment fragment = CheckEmailFragment.newInstance(email);
            switchFragment(fragment, R.id.fragment_register_email, CheckEmailFragment.TAG);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.WELCOME_BACK_EMAIL_FLOW
                || requestCode == RequestCodes.WELCOME_BACK_IDP_FLOW) {
            finish(resultCode, data);
        }
    }

    @Override
    public void onExistingEmailUser(User user) {
        if (user.getProviderId().equals(EMAIL_LINK_PROVIDER)) {
            AuthUI.IdpConfig emailConfig = ProviderUtils.getConfigFromIdpsOrThrow(
                    getFlowParams().providers, EMAIL_LINK_PROVIDER);
            showRegisterEmailLinkFragment(
                    emailConfig, user.getEmail());
        } else {
            startActivityForResult(
                    WelcomeBackPasswordPrompt.createIntent(
                            this, getFlowParams(), new IdpResponse.Builder(user).build()),
                    RequestCodes.WELCOME_BACK_EMAIL_FLOW);
            setSlideAnimation();
        }
    }

    @Override
    public void onExistingIdpUser(User user) {
        // Existing social user, direct them to sign in using their chosen provider.
        startActivityForResult(
                WelcomeBackIdpPrompt.createIntent(this, getFlowParams(), user),
                RequestCodes.WELCOME_BACK_IDP_FLOW);
        setSlideAnimation();
    }

    @Override
    public void onNewUser(User user) {
        // New user, direct them to create an account with email/password
        // if account creation is enabled in SignInIntentBuilder

        TextInputLayout emailLayout = findViewById(R.id.email_layout);
        AuthUI.IdpConfig emailConfig = ProviderUtils.getConfigFromIdps(getFlowParams().providers,
                EmailAuthProvider.PROVIDER_ID);

        if (emailConfig == null) {
            emailConfig = ProviderUtils.getConfigFromIdps(getFlowParams().providers,
                    EMAIL_LINK_PROVIDER);
        }

        if (emailConfig.getParams().getBoolean(ExtraConstants.ALLOW_NEW_EMAILS, true)) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (emailConfig.getProviderId().equals(EMAIL_LINK_PROVIDER)) {
                showRegisterEmailLinkFragment(emailConfig, user.getEmail());
            } else {
                RegisterEmailFragment fragment = RegisterEmailFragment.newInstance(user);
                ft.replace(R.id.fragment_register_email, fragment, RegisterEmailFragment.TAG);
                if (emailLayout != null) {
                    String emailFieldName = getString(R.string.fui_email_field_name);
                    ViewCompat.setTransitionName(emailLayout, emailFieldName);
                    ft.addSharedElement(emailLayout, emailFieldName);
                }
                ft.disallowAddToBackStack().commit();
            }
        } else {
            emailLayout.setError(getString(R.string.fui_error_email_does_not_exist));
        }
    }

    @Override
    public void onTroubleSigningIn(String email) {
        TroubleSigningInFragment troubleSigningInFragment = TroubleSigningInFragment.newInstance
                (email);
        switchFragment(troubleSigningInFragment, R.id.fragment_register_email,
                TroubleSigningInFragment.TAG, true, true);
    }

    @Override
    public void onClickResendEmail(String email) {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            // We're assuming that to get to the TroubleSigningInFragment, we went through
            // the EmailLinkFragment, which was added to the fragment back stack.
            // From here, we're going to register the EmailLinkFragment again, meaning we'd have to
            // pop off the back stack twice to return to the nascar screen. To avoid this,
            // we pre-emptively pop off the last EmailLinkFragment here.
            getSupportFragmentManager().popBackStack();
        }
        AuthUI.IdpConfig emailConfig = ProviderUtils.getConfigFromIdpsOrThrow(
                getFlowParams().providers, EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD);
        showRegisterEmailLinkFragment(
                emailConfig, email);
    }

    @Override
    public void onSendEmailFailure(Exception e) {
        finishOnDeveloperError(e);
    }

    @Override
    public void onDeveloperFailure(Exception e) {
        finishOnDeveloperError(e);
    }

    private void finishOnDeveloperError(Exception e) {
        finish(RESULT_CANCELED, IdpResponse.getErrorIntent(new FirebaseUiException(
                ErrorCodes.DEVELOPER_ERROR, e.getMessage())));
    }

    private void setSlideAnimation() {
        // Make the next activity slide in
        overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
    }

    private void showRegisterEmailLinkFragment(AuthUI.IdpConfig emailConfig,
                                               String email) {
        ActionCodeSettings actionCodeSettings = emailConfig.getParams().getParcelable
                (ExtraConstants.ACTION_CODE_SETTINGS);
        EmailLinkFragment fragment = EmailLinkFragment.newInstance(email,
                actionCodeSettings);
        switchFragment(fragment, R.id.fragment_register_email, EmailLinkFragment.TAG);
    }

    @Override
    public void showProgress(@StringRes int message) {
        throw new UnsupportedOperationException("Email fragments must handle progress updates.");
    }

    @Override
    public void hideProgress() {
        throw new UnsupportedOperationException("Email fragments must handle progress updates.");
    }

    @Override
    public void onMergeFailure(IdpResponse response) {
        finish(ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT, response.toIntent());
    }
}
