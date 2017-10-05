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
import android.support.annotation.RestrictTo;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.provider.EmailProvider;
import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.PhoneProvider;
import com.firebase.ui.auth.provider.Provider;
import com.firebase.ui.auth.provider.TwitterProvider;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;

import java.util.ArrayList;
import java.util.List;

/** Presents the list of authentication options for this app to the user. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AuthMethodPickerActivity extends AppCompatBase {
    private static final String TAG = "AuthMethodPicker";

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return HelperActivityBase.createBaseIntent(
                context, AuthMethodPickerActivity.class, flowParams);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_auth_method_picker_layout);

        FlowParameters params = getFlowHolder().getParams();
        populateIdpList(params.providerInfo);

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

    private void populateIdpList(List<IdpConfig> providerConfigs) {
        List<Provider> providers = new ArrayList<>();
        for (IdpConfig idpConfig : providerConfigs) {
            switch (idpConfig.getProviderId()) {
                case AuthUI.GOOGLE_PROVIDER:
                    providers.add(new GoogleProvider(this, idpConfig));
                    break;
                case AuthUI.FACEBOOK_PROVIDER:
                    providers.add(new FacebookProvider(this, idpConfig));
                    break;
                case AuthUI.TWITTER_PROVIDER:
                    providers.add(new TwitterProvider(this));
                    break;
                case AuthUI.EMAIL_PROVIDER:
                    providers.add(new EmailProvider(this));
                    break;
                case AuthUI.PHONE_VERIFICATION_PROVIDER:
                    providers.add(new PhoneProvider(this, idpConfig));
                    break;
                default:
                    Log.e(TAG, "Encountered unknown provider parcel with type: "
                            + idpConfig.getProviderId());
            }
        }

        ViewGroup btnHolder = findViewById(R.id.btn_holder);
        for (final Provider provider : providers) {
            View loginButton = getLayoutInflater()
                    .inflate(provider.getButtonLayout(), btnHolder, false);

            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    provider.startLogin(AuthMethodPickerActivity.this);
                }
            });
            btnHolder.addView(loginButton);
        }
    }
}
