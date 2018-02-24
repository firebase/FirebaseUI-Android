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
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.provider.EmailProvider;
import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.IdpProvider;
import com.firebase.ui.auth.provider.IdpProvider.IdpCallback;
import com.firebase.ui.auth.provider.PhoneProvider;
import com.firebase.ui.auth.provider.Provider;
import com.firebase.ui.auth.provider.TwitterProvider;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.EmailActivity;
import com.firebase.ui.auth.ui.phone.PhoneActivity;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Presents the list of authentication options for this app to the user. If an identity provider
 * option is selected, a {@link CredentialSignInHandler} is launched to manage the IDP-specific
 * sign-in flow. If email authentication is chosen, the {@link EmailActivity} is started. if
 * phone authentication is chosen, the {@link PhoneActivity}
 * is started.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AuthMethodPickerActivity extends AppCompatBase implements IdpCallback {
    private static final String TAG = "AuthMethodPicker";

    private static final int RC_ACCOUNT_LINK = 3;

    private List<Provider> mProviders;

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return HelperActivityBase.createBaseIntent(
                context, AuthMethodPickerActivity.class, flowParams);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_auth_method_picker_layout);

        populateIdpList(getFlowParams().providerInfo);

        int logoId = getFlowParams().logoId;
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

    private void populateIdpList(List<IdpConfig> providers) {
        mProviders = new ArrayList<>();
        for (IdpConfig idpConfig : providers) {
            switch (idpConfig.getProviderId()) {
                case GoogleAuthProvider.PROVIDER_ID:
                    mProviders.add(new GoogleProvider(this, idpConfig));
                    break;
                case FacebookAuthProvider.PROVIDER_ID:
                    mProviders.add(new FacebookProvider(
                            idpConfig, getFlowParams().themeId));
                    break;
                case TwitterAuthProvider.PROVIDER_ID:
                    mProviders.add(new TwitterProvider(this));
                    break;
                case EmailAuthProvider.PROVIDER_ID:
                    mProviders.add(new EmailProvider(this, getFlowParams()));
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    mProviders.add(new PhoneProvider(this, getFlowParams()));
                    break;
                default:
                    Log.e(TAG, "Encountered unknown provider parcel with type: "
                            + idpConfig.getProviderId());
            }
        }

        ViewGroup btnHolder = findViewById(R.id.btn_holder);
        for (final Provider provider : mProviders) {
            View loginButton = getLayoutInflater()
                    .inflate(provider.getButtonLayout(), btnHolder, false);

            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (provider instanceof IdpProvider) {
                        getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_loading);
                    }
                    provider.startLogin(AuthMethodPickerActivity.this);
                }
            });
            if (provider instanceof IdpProvider) {
                ((IdpProvider) provider).setAuthenticationCallback(this);
            }
            btnHolder.addView(loginButton);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_ACCOUNT_LINK) {
            finish(resultCode, data);
        } else {
            for (Provider provider : mProviders) {
                provider.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onSuccess(@NonNull final IdpResponse response) {
        AuthCredential credential = ProviderUtils.getAuthCredential(response);
        getAuthHelper().getFirebaseAuth()
                .signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        FirebaseUser firebaseUser = authResult.getUser();
                        startSaveCredentials(firebaseUser, null, response);
                    }
                })
                .addOnFailureListener(new CredentialSignInHandler(
                        this, RC_ACCOUNT_LINK, response))
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Firebase sign in with credential " +
                                credential.getProvider() + " unsuccessful. " +
                                "Visit https://console.firebase.google.com to enable it."));
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        // stay on this screen
        getDialogHolder().dismissDialog();
    }
}
