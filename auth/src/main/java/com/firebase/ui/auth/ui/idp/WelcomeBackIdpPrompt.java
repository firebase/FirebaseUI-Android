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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.FacebookSignInHandler;
import com.firebase.ui.auth.data.remote.GenericIdpAnonymousUpgradeLinkingHandler;
import com.firebase.ui.auth.data.remote.GenericIdpSignInHandler;
import com.firebase.ui.auth.data.remote.GoogleSignInHandler;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.firebase.ui.auth.viewmodel.idp.LinkingSocialProviderResponseHandler;
import com.google.android.gms.auth.api.Auth;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.ViewModelProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WelcomeBackIdpPrompt extends AppCompatBase {
    private ProviderSignInBase<?> mProvider;

    private Button mDoneButton;
    private ProgressBar mProgressBar;
    private TextView mPromptText;

    public static Intent createIntent(
            Context context, FlowParameters flowParams, User existingUser) {
        return createIntent(context, flowParams, existingUser, null);
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            User existingUser,
            @Nullable IdpResponse requestedUserResponse) {
        return createBaseIntent(context, WelcomeBackIdpPrompt.class, flowParams)
                .putExtra(ExtraConstants.IDP_RESPONSE, requestedUserResponse)
                .putExtra(ExtraConstants.USER, existingUser);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_welcome_back_idp_prompt_layout);

        mDoneButton = findViewById(R.id.welcome_back_idp_button);
        mProgressBar = findViewById(R.id.top_progress_bar);
        mPromptText = findViewById(R.id.welcome_back_idp_prompt);

        User existingUser = User.getUser(getIntent());
        IdpResponse requestedUserResponse = IdpResponse.fromResultIntent(getIntent());
        ViewModelProvider supplier = new ViewModelProvider(this);

        final LinkingSocialProviderResponseHandler handler =
                supplier.get(LinkingSocialProviderResponseHandler.class);
        handler.init(getFlowParams());
        if (requestedUserResponse != null) {
            handler.setRequestedSignInCredentialForEmail(
                    ProviderUtils.getAuthCredential(requestedUserResponse),
                    existingUser.getEmail());
        }

        final String providerId = existingUser.getProviderId();
        AuthUI.IdpConfig config =
                ProviderUtils.getConfigFromIdps(getFlowParams().providers, providerId);
        if (config == null) {
            finish(RESULT_CANCELED, IdpResponse.getErrorIntent(new FirebaseUiException(
                    ErrorCodes.DEVELOPER_ERROR,
                    "Firebase login unsuccessful."
                            + " Account linking failed due to provider not enabled by application: "
                            + providerId)));
            return;
        }


        String providerName;

        String genericOAuthProviderId = config.getParams()
                .getString(ExtraConstants.GENERIC_OAUTH_PROVIDER_ID);

        boolean useEmulator = getAuthUI().isUseEmulator();

        switch (providerId) {
            case GoogleAuthProvider.PROVIDER_ID:
                if (useEmulator) {
                    mProvider = supplier.get(GenericIdpAnonymousUpgradeLinkingHandler.class)
                            .initWith(GenericIdpSignInHandler.getGenericGoogleConfig());
                } else {
                    mProvider = supplier.get(GoogleSignInHandler.class).initWith(
                            new GoogleSignInHandler.Params(config, existingUser.getEmail()));
                }
                providerName = getString(R.string.fui_idp_name_google);
                break;
            case FacebookAuthProvider.PROVIDER_ID:
                if (useEmulator) {
                    mProvider = supplier.get(GenericIdpAnonymousUpgradeLinkingHandler.class)
                            .initWith(GenericIdpSignInHandler.getGenericFacebookConfig());
                } else {
                    mProvider = supplier.get(FacebookSignInHandler.class).initWith(config);
                }
                providerName = getString(R.string.fui_idp_name_facebook);
                break;
            default:
                if (TextUtils.equals(providerId, genericOAuthProviderId)) {
                    mProvider = supplier.get(GenericIdpAnonymousUpgradeLinkingHandler.class)
                            .initWith(config);
                    providerName = config.getParams()
                            .getString(ExtraConstants.GENERIC_OAUTH_PROVIDER_NAME);
                } else {
                    throw new IllegalStateException("Invalid provider id: " + providerId);
                }
        }

        mProvider.getOperation().observe(this, new ResourceObserver<IdpResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdpResponse response) {
                boolean isGenericIdp = getAuthUI().isUseEmulator()
                        || !AuthUI.SOCIAL_PROVIDERS.contains(response.getProviderType());

                if (isGenericIdp
                        && !response.hasCredentialForLinking()
                        && !handler.hasCredentialForLinking()) {
                    // Generic Idp does not return a credential - if this is not a linking flow,
                    // the user is already signed in and we are done.
                    finish(RESULT_OK, response.toIntent());
                    return;
                }
                handler.startSignIn(response);
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                handler.startSignIn(IdpResponse.from(e));
            }
        });

        mPromptText.setText(getString(
                R.string.fui_welcome_back_idp_prompt,
                existingUser.getEmail(),
                providerName));

        mDoneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mProvider.startSignIn(getAuth(), WelcomeBackIdpPrompt.this, providerId);
            }
        });

        handler.getOperation().observe(this, new ResourceObserver<IdpResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdpResponse response) {
                finish(RESULT_OK, response.toIntent());
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                if (e instanceof FirebaseAuthAnonymousUpgradeException) {
                    IdpResponse response = ((FirebaseAuthAnonymousUpgradeException) e).getResponse();
                    finish(ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT, response.toIntent());
                } else {
                    finish(RESULT_CANCELED, IdpResponse.getErrorIntent(e));
                }
            }
        });

        TextView footerText = findViewById(R.id.email_footer_tos_and_pp_text);
        PrivacyDisclosureUtils.setupTermsOfServiceFooter(this, getFlowParams(), footerText);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mProvider.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void showProgress(int message) {
        mDoneButton.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        mDoneButton.setEnabled(true);
        mProgressBar.setVisibility(View.INVISIBLE);
    }
}
