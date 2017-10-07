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

package com.firebase.ui.auth.ui.accountlink;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.ProviderDisabledException;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.provider.FacebookProvider;
import com.firebase.ui.auth.ui.provider.GoogleProvider;
import com.firebase.ui.auth.ui.provider.Provider;
import com.firebase.ui.auth.ui.provider.TwitterProvider;
import com.firebase.ui.auth.util.ExtraConstants;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WelcomeBackIdpPrompt extends AppCompatBase {
    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            User user,
            @Nullable IdpResponse response) {
        return HelperActivityBase.createBaseIntent(context, WelcomeBackIdpPrompt.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_USER, user)
                .putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, response);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_welcome_back_idp_prompt_layout);

        User oldUser = User.getUser(getIntent());
        Provider idpProvider = null;

        String providerId = oldUser.getProviderId();
        for (IdpConfig idpConfig : getFlowHolder().getParams().providerInfo) {
            if (providerId.equals(idpConfig.getProviderId())) {
                switch (providerId) {
                    case GoogleAuthProvider.PROVIDER_ID:
                        idpProvider = new GoogleProvider(this, idpConfig, oldUser.getEmail());
                        break;
                    case FacebookAuthProvider.PROVIDER_ID:
                        idpProvider = new FacebookProvider(this, idpConfig);
                        break;
                    case TwitterAuthProvider.PROVIDER_ID:
                        idpProvider = new TwitterProvider(this);
                        break;
                    default:
                        throw new IllegalStateException("Unknown provider: " + providerId);
                }
            }
        }

        if (idpProvider == null) {
            finish(RESULT_CANCELED, IdpResponse.getErrorIntent(
                    new ProviderDisabledException(providerId)));
            return;
        }

        ((TextView) findViewById(R.id.welcome_back_idp_prompt)).setText(getString(
                R.string.fui_welcome_back_idp_prompt,
                oldUser.getEmail(),
                idpProvider.getName(this)));

        final Provider finalIdpProvider = idpProvider;
        findViewById(R.id.welcome_back_idp_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_signing_in);
                finalIdpProvider.startLogin(WelcomeBackIdpPrompt.this);
            }
        });

        getSignInHandler().getSuccessLiveData().observe(this, new Observer<IdpResponse>() {
            @Override
            public void onChanged(@Nullable IdpResponse response) {
                finish(Activity.RESULT_OK, response.toIntent());
            }
        });
        getSignInHandler().getFailureLiveData().observe(this, new Observer<FirebaseAuthException>() {
            @Override
            public void onChanged(@Nullable FirebaseAuthException e) {
                finish(Activity.RESULT_CANCELED, IdpResponse.getErrorIntent(e));
            }
        });
    }
}
