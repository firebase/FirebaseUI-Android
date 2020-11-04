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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.firebase.ui.auth.data.remote.GenericIdpSignInHandler;
import com.firebase.ui.auth.data.remote.GoogleSignInHandler;
import com.firebase.ui.auth.data.remote.PhoneSignInHandler;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.firebase.ui.auth.viewmodel.idp.SocialProviderResponseHandler;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.lifecycle.ViewModelProvider;

import static com.firebase.ui.auth.util.ExtraConstants.GENERIC_OAUTH_BUTTON_ID;
import static com.firebase.ui.auth.util.ExtraConstants.GENERIC_OAUTH_PROVIDER_ID;
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

        mHandler = new ViewModelProvider(this).get(SocialProviderResponseHandler.class);
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
                if (e instanceof UserCancellationException) {
                    // User pressed back, there is no error.
                    return;
                }

                if (e instanceof FirebaseAuthAnonymousUpgradeException) {
                    finish(ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT,
                            ((FirebaseAuthAnonymousUpgradeException) e).getResponse().toIntent());
                } else if (e instanceof FirebaseUiException) {
                    FirebaseUiException fue = (FirebaseUiException) e;
                    finish(RESULT_CANCELED, IdpResponse.from(fue).toIntent());
                } else {
                    String text = getString(R.string.fui_error_unknown);
                    Toast.makeText(AuthMethodPickerActivity.this,
                            text,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void populateIdpList(List<IdpConfig> providerConfigs) {

        ViewModelProvider supplier = new ViewModelProvider(this);
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
                    if (!TextUtils.isEmpty(
                            idpConfig.getParams().getString(GENERIC_OAUTH_PROVIDER_ID))) {
                        buttonLayout = idpConfig.getParams().getInt(GENERIC_OAUTH_BUTTON_ID);
                        break;
                    }
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

            Integer buttonResId = providerButtonIds.get(providerId);
            if (buttonResId == null) {
                throw new IllegalStateException("No button found for auth provider: " + idpConfig.getProviderId());
            }

            @IdRes int buttonId = buttonResId;
            View loginButton = findViewById(buttonId);
            handleSignInOperation(idpConfig, loginButton);
        }
        //hide custom layout buttons that don't have their identity provider set
        for (String providerBtnId : providerButtonIds.keySet()) {
            if (providerBtnId == null) {
                continue;
            }
            boolean hasProvider = false;
            for (IdpConfig idpConfig : providerConfigs) {
                if (providerBtnId.equals(idpConfig.getProviderId())) {
                    hasProvider = true;
                    break;
                }
            }
            if (!hasProvider) {
                Integer resId = providerButtonIds.get(providerBtnId);
                if (resId == null) {
                    continue;
                }
                @IdRes int buttonId = resId;
                findViewById(buttonId).setVisibility(View.GONE);
            }
        }
    }

    @NonNull
    private String providerOrEmailLinkProvider(@NonNull String providerId) {
        if (providerId.equals(EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD)) {
            return EmailAuthProvider.PROVIDER_ID;
        }

        return providerId;
    }

    private void handleSignInOperation(final IdpConfig idpConfig, View view) {
        ViewModelProvider supplier = new ViewModelProvider(this);
        final String providerId = idpConfig.getProviderId();
        final ProviderSignInBase<?> provider;

        AuthUI authUI = getAuthUI();

        switch (providerId) {
            case EMAIL_LINK_PROVIDER:
            case EmailAuthProvider.PROVIDER_ID:
                provider = supplier.get(EmailSignInHandler.class).initWith(null);
                break;
            case PhoneAuthProvider.PROVIDER_ID:
                provider = supplier.get(PhoneSignInHandler.class).initWith(idpConfig);
                break;
            case AuthUI.ANONYMOUS_PROVIDER:
                provider = supplier.get(AnonymousSignInHandler.class).initWith(getFlowParams());
                break;
            case GoogleAuthProvider.PROVIDER_ID:
                if (authUI.isUseEmulator()) {
                    provider = supplier.get(GenericIdpSignInHandler.class)
                            .initWith(GenericIdpSignInHandler.getGenericGoogleConfig());
                } else {
                    provider = supplier.get(GoogleSignInHandler.class).initWith(
                            new GoogleSignInHandler.Params(idpConfig));
                }
                break;
            case FacebookAuthProvider.PROVIDER_ID:
                if (authUI.isUseEmulator()) {
                    provider = supplier.get(GenericIdpSignInHandler.class)
                            .initWith(GenericIdpSignInHandler.getGenericFacebookConfig());
                } else {
                    provider = supplier.get(FacebookSignInHandler.class).initWith(idpConfig);
                }
                break;
            default:
                if (!TextUtils.isEmpty(
                        idpConfig.getParams().getString(GENERIC_OAUTH_PROVIDER_ID))) {
                    provider = supplier.get(GenericIdpSignInHandler.class).initWith(idpConfig);
                    break;
                }
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
                if (e instanceof FirebaseAuthAnonymousUpgradeException) {
                    finish(RESULT_CANCELED, new Intent().putExtra(ExtraConstants.IDP_RESPONSE,
                            IdpResponse.from(e)));
                    return;
                }
                handleResponse(IdpResponse.from(e));
            }

            private void handleResponse(@NonNull IdpResponse response) {
                // If we're using the emulator then the social flows actually use Generic IDP
                // instead which means we shouldn't use the social response handler.
                boolean isSocialResponse = AuthUI.SOCIAL_PROVIDERS.contains(providerId)
                        && !getAuthUI().isUseEmulator();

                if (!response.isSuccessful()) {
                    // We have no idea what provider this error stemmed from so just forward
                    // this along to the handler.
                    mHandler.startSignIn(response);
                } else if (isSocialResponse) {
                    // Don't use the response's provider since it can be different than the one
                    // that launched the sign-in attempt. Ex: the email flow is started, but
                    // ends up turning into a Google sign-in because that account already
                    // existed. In the previous example, an extra sign-in would incorrectly
                    // started.
                    mHandler.startSignIn(response);
                } else {
                    // Email, phone, or generic: the credentials should have already been saved so
                    // simply move along.
                    // Anononymous sign in also does not require any other operations.
                    finish(response.isSuccessful() ? RESULT_OK : RESULT_CANCELED,
                            response.toIntent());
                }
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isOffline()) {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.fui_no_internet), Snackbar.LENGTH_SHORT).show();
                    return;
                }

                provider.startSignIn(getAuth(), AuthMethodPickerActivity.this,
                        idpConfig.getProviderId());
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
