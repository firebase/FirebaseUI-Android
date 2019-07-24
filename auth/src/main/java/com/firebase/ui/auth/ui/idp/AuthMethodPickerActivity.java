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

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.data.remote.AnonymousSignInHandler;
import com.firebase.ui.auth.data.remote.EmailSignInHandler;
import com.firebase.ui.auth.data.remote.FacebookSignInHandler;
import com.firebase.ui.auth.data.remote.GitHubSignInHandlerBridge;
import com.firebase.ui.auth.data.remote.GoogleSignInHandler;
import com.firebase.ui.auth.data.remote.PhoneSignInHandler;
import com.firebase.ui.auth.data.remote.TwitterSignInHandler;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.firebase.ui.auth.viewmodel.idp.SocialProviderResponseHandler;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.firebase.ui.auth.AuthUI.EMAIL_LINK_PROVIDER;

/**
 * Presents the list of authentication options for this app to the user.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AuthMethodPickerActivity extends AppCompatBase {

    private SocialProviderResponseHandler mHandler;
    private List<ProviderSignInBase<?>> mProviders;

    private ProgressBar mProgressBar;
    private ViewGroup mProviderHolder;

    private AuthMethodPickerLayout customLayout;

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return createBaseIntent(context, AuthMethodPickerActivity.class, flowParams);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FlowParameters params = getFlowParams();
        customLayout = params.authMethodPickerLayout;

        mHandler = ViewModelProviders.of(this).get(SocialProviderResponseHandler.class);
        mHandler.init(params);


        mProviders = new ArrayList<>();
        if (customLayout != null) {
            setContentView(customLayout.getMainLayout());

            //Setup using custom layout
            populateIdpListCustomLayout(params.providers);
        } else {
            setContentView(R.layout.fui_auth_method_picker_layout);

            //UI only with default layout
            mProgressBar = findViewById(R.id.top_progress_bar);
            mProviderHolder = findViewById(R.id.btn_holder);

            populateIdpList(params.providers);

            int logoId = params.logoId;
            if (logoId == AuthUI.NO_LOGO) {
                findViewById(R.id.logo).setVisibility(View.GONE);

                ConstraintLayout layout = findViewById(R.id.root);
                ConstraintSet constraints = new ConstraintSet();
                constraints.clone(layout);
                constraints.setHorizontalBias(R.id.container, 0.5f);
                constraints.setVerticalBias(R.id.container, 0.5f);
                constraints.applyTo(layout);
            } else {
                ImageView logo = findViewById(R.id.logo);
                logo.setImageResource(logoId);
            }
        }

        boolean tosAndPpConfigured = getFlowParams().isPrivacyPolicyUrlProvided()
                && getFlowParams().isTermsOfServiceUrlProvided();

        int termsTextId = customLayout == null
                ? R.id.main_tos_and_pp
                : customLayout.getTosPpView();

        if (termsTextId >= 0) {
            TextView termsText = findViewById(termsTextId);

            // No ToS or PP provided, so we should hide the view entirely
            if (!tosAndPpConfigured) {
                termsText.setVisibility(View.GONE);
            } else {
                PrivacyDisclosureUtils.setupTermsOfServiceAndPrivacyPolicyText(this,
                        getFlowParams(),
                        termsText);
            }
        }

        //Handler for both
        mHandler.getOperation().observe(this, new ResourceObserver<IdpResponse>(
                this, R.string.fui_progress_dialog_signing_in) {
            @Override
            protected void onSuccess(@NonNull IdpResponse response) {
                startSaveCredentials(mHandler.getCurrentUser(), response, null);
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                if (e instanceof FirebaseAuthAnonymousUpgradeException) {
                    finish(ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT,
                            ((FirebaseAuthAnonymousUpgradeException) e).getResponse().toIntent());
                } else if ((!(e instanceof UserCancellationException))) {
                    String text = e instanceof FirebaseUiException ? e.getMessage() :
                            getString(R.string.fui_error_unknown);
                    Toast.makeText(AuthMethodPickerActivity.this,
                            text,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void populateIdpList(List<IdpConfig> providerConfigs) {

        ViewModelProvider supplier = ViewModelProviders.of(this);
        mProviders = new ArrayList<>();
        for (IdpConfig idpConfig : providerConfigs) {
            @LayoutRes int buttonLayout;

            final String providerId = idpConfig.getProviderId();
            switch (providerId) {
                case GoogleAuthProvider.PROVIDER_ID:
                    buttonLayout = R.layout.fui_idp_button_google;
                    break;
                case FacebookAuthProvider.PROVIDER_ID:
                    buttonLayout = R.layout.fui_idp_button_facebook;
                    break;
                case TwitterAuthProvider.PROVIDER_ID:
                    buttonLayout = R.layout.fui_idp_button_twitter;
                    break;
                case GithubAuthProvider.PROVIDER_ID:
                    buttonLayout = R.layout.fui_idp_button_github;
                    break;
                case EMAIL_LINK_PROVIDER:
                case EmailAuthProvider.PROVIDER_ID:
                    buttonLayout = R.layout.fui_provider_button_email;
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    buttonLayout = R.layout.fui_provider_button_phone;
                    break;
                case AuthUI.ANONYMOUS_PROVIDER:
                    buttonLayout = R.layout.fui_provider_button_anonymous;
                    break;
                default:
                    throw new IllegalStateException("Unknown provider: " + providerId);
            }

            View loginButton = getLayoutInflater().inflate(buttonLayout, mProviderHolder, false);
            handleSignInOperation(idpConfig, loginButton);
            mProviderHolder.addView(loginButton);
        }
    }

    private void populateIdpListCustomLayout(List<IdpConfig> providerConfigs) {
        Map<String, Integer> providerButtonIds = customLayout.getProvidersButton();
        for (IdpConfig idpConfig : providerConfigs) {
            final String providerId = providerOrEmailLinkProvider(idpConfig.getProviderId());

            if (!providerButtonIds.containsKey(providerId)) {
                throw new IllegalStateException("No button found for auth provider: " + providerId);
            }

            @IdRes int buttonId = providerButtonIds.get(providerId);
            View loginButton = findViewById(buttonId);
            handleSignInOperation(idpConfig, loginButton);
        }
    }

    @NonNull
    private String providerOrEmailLinkProvider(@NonNull String providerId) {
        if (providerId.equals(EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD)) {
            return EmailAuthProvider.PROVIDER_ID;
        }

        return providerId;
    }

    private void handleSignInOperation(IdpConfig idpConfig, View view) {
        ViewModelProvider supplier = ViewModelProviders.of(this);
        final String providerId = idpConfig.getProviderId();
        final ProviderSignInBase<?> provider;

        switch (providerId) {
            case GoogleAuthProvider.PROVIDER_ID:
                GoogleSignInHandler google = supplier.get(GoogleSignInHandler.class);
                google.init(new GoogleSignInHandler.Params(idpConfig));
                provider = google;

                break;
            case FacebookAuthProvider.PROVIDER_ID:
                FacebookSignInHandler facebook = supplier.get(FacebookSignInHandler.class);
                facebook.init(idpConfig);
                provider = facebook;

                break;
            case TwitterAuthProvider.PROVIDER_ID:
                TwitterSignInHandler twitter = supplier.get(TwitterSignInHandler.class);
                twitter.init(null);
                provider = twitter;

                break;
            case GithubAuthProvider.PROVIDER_ID:
                ProviderSignInBase<IdpConfig> github =
                        supplier.get(GitHubSignInHandlerBridge.HANDLER_CLASS);
                github.init(idpConfig);
                provider = github;

                break;
            case EMAIL_LINK_PROVIDER:
            case EmailAuthProvider.PROVIDER_ID:
                EmailSignInHandler email = supplier.get(EmailSignInHandler.class);
                email.init(null);
                provider = email;

                break;
            case PhoneAuthProvider.PROVIDER_ID:
                PhoneSignInHandler phone = supplier.get(PhoneSignInHandler.class);
                phone.init(idpConfig);
                provider = phone;

                break;
            case AuthUI.ANONYMOUS_PROVIDER:
                AnonymousSignInHandler anonymous = supplier.get(AnonymousSignInHandler.class);
                anonymous.init(getFlowParams());
                provider = anonymous;

                break;
            default:
                throw new IllegalStateException("Unknown provider: " + providerId);
        }
        mProviders.add(provider);

        provider.getOperation().observe(this, new ResourceObserver<IdpResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdpResponse response) {
                handleResponse(response);
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                handleResponse(IdpResponse.from(e));
            }

            private void handleResponse(@NonNull IdpResponse response) {
                if (!response.isSuccessful()) {
                    // We have no idea what provider this error stemmed from so just forward
                    // this along to the handler.
                    mHandler.startSignIn(response);
                } else if (AuthUI.SOCIAL_PROVIDERS.contains(providerId)) {
                    // Don't use the response's provider since it can be different than the one
                    // that launched the sign-in attempt. Ex: the email flow is started, but
                    // ends up turning into a Google sign-in because that account already
                    // existed. In the previous example, an extra sign-in would incorrectly
                    // started.
                    mHandler.startSignIn(response);
                } else {
                    // Email or phone: the credentials should have already been saved so
                    // simply move along. Anononymous sign in also does not require any
                    // other operations.
                    finish(response.isSuccessful() ? RESULT_OK : RESULT_CANCELED,
                            response.toIntent());
                }
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isOffline()) {
                    Toast.makeText(AuthMethodPickerActivity.this,
                            getString(R.string.fui_no_internet),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                provider.startSignIn(AuthMethodPickerActivity.this);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mHandler.onActivityResult(requestCode, resultCode, data);
        for (ProviderSignInBase<?> provider : mProviders) {
            provider.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void showProgress(int message) {
        //mProgressBar & mProviderHolder might be null if using custom AuthMethodPickerLayout
        if (customLayout == null) {
            mProgressBar.setVisibility(View.VISIBLE);
            for (int i = 0; i < mProviderHolder.getChildCount(); i++) {
                View child = mProviderHolder.getChildAt(i);
                child.setEnabled(false);
                child.setAlpha(0.75f);
            }
        }
    }

    @Override
    public void hideProgress() {
        //mProgressBar & mProviderHolder might be null if using custom AuthMethodPickerLayout
        if (customLayout == null) {
            mProgressBar.setVisibility(View.INVISIBLE);
            for (int i = 0; i < mProviderHolder.getChildCount(); i++) {
                View child = mProviderHolder.getChildAt(i);
                child.setEnabled(true);
                child.setAlpha(1.0f);
            }
        }
    }
}
