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
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.provider.ProviderUtils;
import com.firebase.ui.auth.provider.EmailProvider;
import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.IdpProvider;
import com.firebase.ui.auth.provider.IdpProvider.IdpCallback;
import com.firebase.ui.auth.provider.PhoneProvider;
import com.firebase.ui.auth.provider.Provider;
import com.firebase.ui.auth.provider.TwitterProvider;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.RegisterEmailActivity;
import com.firebase.ui.auth.ui.phone.PhoneVerificationActivity;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.firebase.auth.AuthCredential;

import java.util.ArrayList;
import java.util.List;

/**
 * Presents the list of authentication options for this app to the user. If an
 * identity provider option is selected, a {@link CredentialSignInHandler}
 * is launched to manage the IDP-specific sign-in flow. If email authentication is chosen,
 * the {@link RegisterEmailActivity} is started. if phone authentication is chosen, the
 * {@link com.firebase.ui.auth.ui.phone.PhoneVerificationActivity} is started.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AuthMethodPickerActivity extends AppCompatBase implements IdpCallback {
    private static final String TAG = "AuthMethodPicker";

    private static final int RC_ACCOUNT_LINK = 3;

    private List<Provider> mProviders;
    @Nullable
    private SaveSmartLock mSaveSmartLock;

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return BaseHelper.createBaseIntent(context, AuthMethodPickerActivity.class, flowParams);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_method_picker_layout);
        mSaveSmartLock = mActivityHelper.getSaveSmartLockInstance(this);

        populateIdpList(mActivityHelper.getFlowParams().providerInfo);

        int logoId = mActivityHelper.getFlowParams().logoId;
        if (logoId == AuthUI.NO_LOGO) {
            findViewById(R.id.logo_layout).setVisibility(View.GONE);
        } else {
            ImageView logo = (ImageView) findViewById(R.id.logo);
            logo.setImageResource(logoId);
        }
    }

    private void populateIdpList(List<IdpConfig> providers) {
        mProviders = new ArrayList<>();
        for (IdpConfig idpConfig : providers) {
            switch (idpConfig.getProviderId()) {
                case AuthUI.GOOGLE_PROVIDER:
                    mProviders.add(new GoogleProvider(this, idpConfig));
                    break;
                case AuthUI.FACEBOOK_PROVIDER:
                    mProviders.add(new FacebookProvider(
                            idpConfig, mActivityHelper.getFlowParams().themeId));
                    break;
                case AuthUI.TWITTER_PROVIDER:
                    mProviders.add(new TwitterProvider(this));
                    break;
                case AuthUI.EMAIL_PROVIDER:
                    mProviders.add(new EmailProvider(this, mActivityHelper));
                    break;
                case AuthUI.PHONE_VERIFICATION_PROVIDER:
                    mProviders.add(new PhoneProvider(this, mActivityHelper));
                    break;
                default:
                    Log.e(TAG, "Encountered unknown provider parcel with type: "
                            + idpConfig.getProviderId());
            }
        }

        ViewGroup btnHolder = (ViewGroup) findViewById(R.id.btn_holder);
        for (final Provider provider : mProviders) {
            View loginButton = getLayoutInflater()
                    .inflate(provider.getButtonLayout(), btnHolder, false);

            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (provider instanceof IdpProvider) {
                        mActivityHelper.showLoadingDialog(R.string.progress_dialog_loading);
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
    public void onSuccess(final IdpResponse response) {
        AuthCredential credential = ProviderUtils.getAuthCredential(response);
        mActivityHelper.getFirebaseAuth()
                .signInWithCredential(credential)
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Firebase sign in with credential "
                                + credential.getProvider() + " unsuccessful. " +
                                "Visit https://console.firebase.google.com to enable it."))
                .addOnCompleteListener(new CredentialSignInHandler(
                        this,
                        mActivityHelper,
                        mSaveSmartLock,
                        RC_ACCOUNT_LINK,
                        response));
    }

    @Override
    public void onFailure(Bundle extra) {
        // stay on this screen
        mActivityHelper.dismissDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProviders != null) {
            for (Provider provider : mProviders) {
                if (provider instanceof GoogleProvider) {
                    ((GoogleProvider) provider).disconnect();
                }
            }
        }
    }
}
