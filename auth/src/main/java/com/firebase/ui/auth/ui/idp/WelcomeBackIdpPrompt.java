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

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.FacebookSignInHandler;
import com.firebase.ui.auth.data.remote.GoogleSignInHandler;
import com.firebase.ui.auth.data.remote.TwitterSignInHandler;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.ui.FlowUtils;
import com.firebase.ui.auth.viewmodel.idp.LinkingSocialProviderResponseHandler;
import com.firebase.ui.auth.viewmodel.idp.ProviderSignInBase;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WelcomeBackIdpPrompt extends AppCompatBase {
    private ProviderSignInBase<?> mProvider;

    public static Intent createIntent(
            Context context, FlowParameters flowParams, User existingUser) {
        return createIntent(context, flowParams, existingUser, null);
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            User existingUser,
            @Nullable IdpResponse requestedUserResponse) {
        return HelperActivityBase.createBaseIntent(context, WelcomeBackIdpPrompt.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, requestedUserResponse)
                .putExtra(ExtraConstants.EXTRA_USER, existingUser);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_welcome_back_idp_prompt_layout);

        User existingUser = User.getUser(getIntent());
        IdpResponse requestedUserResponse = IdpResponse.fromResultIntent(getIntent());
        ViewModelProvider supplier = ViewModelProviders.of(this);

        final LinkingSocialProviderResponseHandler handler =
                supplier.get(LinkingSocialProviderResponseHandler.class);
        handler.init(getFlowParams());
        if (requestedUserResponse != null) {
            handler.setRequestedSignInCredential(
                    ProviderUtils.getAuthCredential(requestedUserResponse));
        }

        String providerId = existingUser.getProviderId();
        AuthUI.IdpConfig config =
                ProviderUtils.getConfigFromIdps(getFlowParams().providerInfo, providerId);
        if (config == null) {
            finish(RESULT_CANCELED, IdpResponse.getErrorIntent(new FirebaseUiException(
                    ErrorCodes.DEVELOPER_ERROR,
                    "Firebase login unsuccessful."
                            + " Account linking failed due to provider not enabled by application: "
                            + providerId)));
            return;
        }

        @StringRes int providerName;
        switch (providerId) {
            case GoogleAuthProvider.PROVIDER_ID:
                GoogleSignInHandler google = supplier.get(GoogleSignInHandler.class);
                google.init(new GoogleSignInHandler.Params(config, existingUser.getEmail()));
                mProvider = google;

                providerName = R.string.fui_idp_name_google;
                break;
            case FacebookAuthProvider.PROVIDER_ID:
                FacebookSignInHandler facebook = supplier.get(FacebookSignInHandler.class);
                facebook.init(config);
                mProvider = facebook;

                providerName = R.string.fui_idp_name_facebook;
                break;
            case TwitterAuthProvider.PROVIDER_ID:
                TwitterSignInHandler twitter = supplier.get(TwitterSignInHandler.class);
                twitter.init(null);
                mProvider = twitter;

                providerName = R.string.fui_idp_name_twitter;
                break;
            default:
                throw new IllegalStateException("Invalid provider id: " + providerId);
        }

        mProvider.getOperation().observe(this, new Observer<Resource<IdpResponse>>() {
            @Override
            public void onChanged(Resource<IdpResponse> resource) {
                if (resource.getState() == State.LOADING) {
                    getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_loading);
                    return;
                }
                getDialogHolder().dismissDialog();

                if (resource.getState() == State.SUCCESS
                        || resource.getState() == State.FAILURE) {
                    handler.startSignIn(IdpResponse.from(resource));
                }
            }
        });

        ((TextView) findViewById(R.id.welcome_back_idp_prompt)).setText(getString(
                R.string.fui_welcome_back_idp_prompt,
                existingUser.getEmail(),
                getString(providerName)));

        findViewById(R.id.welcome_back_idp_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mProvider.startSignIn(WelcomeBackIdpPrompt.this);
            }
        });

        handler.getOperation().observe(this, new Observer<Resource<IdpResponse>>() {
            @Override
            public void onChanged(Resource<IdpResponse> resource) {
                if (resource.getState() == State.LOADING) {
                    getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_loading);
                    return;
                }
                getDialogHolder().dismissDialog();

                if (resource.getState() == State.SUCCESS) {
                    finish(RESULT_OK, resource.getValue().toIntent());
                } else if (resource.getState() == State.FAILURE) {
                    Exception e = resource.getException();
                    if (!FlowUtils.handleError(WelcomeBackIdpPrompt.this, e)) {
                        finish(RESULT_CANCELED, IdpResponse.getErrorIntent(e));
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mProvider.onActivityResult(requestCode, resultCode, data);
    }
}
