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
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.RestrictTo;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.provider.EmailProvider;
import com.firebase.ui.auth.ui.provider.FacebookProvider;
import com.firebase.ui.auth.ui.provider.GoogleProvider;
import com.firebase.ui.auth.ui.provider.PhoneProvider;
import com.firebase.ui.auth.ui.provider.ProviderBase;
import com.firebase.ui.auth.ui.provider.TwitterProvider;
import com.firebase.ui.auth.util.ui.FlowUtils;
import com.firebase.ui.auth.viewmodel.idp.ProviderResponseHandlerBase;
import com.firebase.ui.auth.viewmodel.idp.SimpleProviderResponseHandler;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.ArrayList;
import java.util.List;

/** Presents the list of authentication options for this app to the user. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AuthMethodPickerActivity extends AppCompatBase {
    private SimpleProviderResponseHandler mHandler;
    private List<ProviderBase> mProviders;

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return HelperActivityBase.createBaseIntent(
                context, AuthMethodPickerActivity.class, flowParams);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_auth_method_picker_layout);

        FlowParameters params = getFlowParams();
        mHandler = ViewModelProviders.of(this).get(SimpleProviderResponseHandler.class);
        mHandler.init(params);

        populateIdpList(params.providerInfo, mHandler);

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

        mHandler.getOperation().observe(this, new Observer<Resource<IdpResponse>>() {
            @Override
            public void onChanged(Resource<IdpResponse> resource) {
                if (resource.getState() == State.LOADING) {
                    getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_signing_in);
                    return;
                }
                getDialogHolder().dismissDialog();

                if (resource.getState() == State.SUCCESS) {
                    startSaveCredentials(mHandler.getCurrentUser(), null, resource.getValue());
                } else if (resource.getState() == State.FAILURE) {
                    Exception e = resource.getException();
                    if (!FlowUtils.handleError(AuthMethodPickerActivity.this, e)
                            && !(e instanceof UserCancellationException)) {
                        Toast.makeText(AuthMethodPickerActivity.this,
                                R.string.fui_error_unknown,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void populateIdpList(List<IdpConfig> providerConfigs,
                                 final ProviderResponseHandlerBase handler) {
        ViewGroup providerHolder = findViewById(R.id.btn_holder);

        mProviders = new ArrayList<>();
        for (IdpConfig idpConfig : providerConfigs) {
            final ProviderBase provider;
            switch (idpConfig.getProviderId()) {
                case GoogleAuthProvider.PROVIDER_ID:
                    provider = new GoogleProvider(this);
                    break;
                case FacebookAuthProvider.PROVIDER_ID:
                    provider = new FacebookProvider(this);
                    break;
                case TwitterAuthProvider.PROVIDER_ID:
                    provider = new TwitterProvider(this);
                    break;
                case EmailAuthProvider.PROVIDER_ID:
                    provider = new EmailProvider();
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    provider = new PhoneProvider(idpConfig);
                    break;
                default:
                    throw new IllegalStateException("Unknown provider: " + idpConfig.getProviderId());
            }
            mProviders.add(provider);

            provider.getResponseListener().observe(this, new Observer<IdpResponse>() {
                @Override
                public void onChanged(IdpResponse response) {
                    handler.startSignIn(response);
                }
            });

            View loginButton = getLayoutInflater()
                    .inflate(provider.getButtonLayout(), providerHolder, false);
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    provider.startLogin(AuthMethodPickerActivity.this);
                }
            });
            providerHolder.addView(loginButton);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mHandler.onActivityResult(requestCode, resultCode, data);
        for (ProviderBase provider : mProviders) {
            provider.onActivityResult(requestCode, resultCode, data);
        }
    }
}
