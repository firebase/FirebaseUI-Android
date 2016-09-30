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
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.IDPProvider;
import com.firebase.ui.auth.provider.IDPProviderParcel;
import com.firebase.ui.auth.provider.IDPResponse;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.EmailHintContainerActivity;
import com.firebase.ui.auth.util.EmailFlowUtil;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Presents the list of authentication options for this app to the user. If an
 * identity provider option is selected, a {@link IDPSignInContainerActivity container activity}
 * is launched to manage the IDP-specific sign-in flow. If email authentication is chosen,
 * the {@link EmailHintContainerActivity root email flow activity} is started.
 *
 * <p style="text-align: center">
 * <img alt="Authentication picker activity rendered with all authentication options and default settings"
 *      src="data:image/png;base64,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">
 */
public class AuthMethodPickerActivity
        extends IDPBaseActivity
        implements IDPProvider.IDPCallback, View.OnClickListener {

    private static final int RC_EMAIL_FLOW = 2;
    private static final int RC_ACCOUNT_LINK = 3;
    private static final int RC_SAVE_CREDENTIAL = 4;
    private static final String TAG = "AuthMethodPicker";
    private ArrayList<IDPProvider> mIdpProviders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_method_picker_layout);

        findViewById(R.id.email_provider).setOnClickListener(this);

        populateIdpList(mActivityHelper.getFlowParams().providerInfo);

        int logoId = mActivityHelper.getFlowParams().logoId;
        ImageView logo = (ImageView) findViewById(R.id.logo);
        if (logoId == AuthUI.NO_LOGO) {
            logo.setVisibility(View.GONE);
        } else {
            logo.setImageResource(logoId);
        }
    }

    private void populateIdpList(List<IDPProviderParcel> providers) {
        mIdpProviders = new ArrayList<>();
        for (IDPProviderParcel providerParcel : providers) {
            switch (providerParcel.getProviderType()) {
                case FacebookAuthProvider.PROVIDER_ID :
                    mIdpProviders.add(new FacebookProvider(this, providerParcel));
                    break;
                case GoogleAuthProvider.PROVIDER_ID:
                    mIdpProviders.add(new GoogleProvider(this, providerParcel, null));
                    break;
                case EmailAuthProvider.PROVIDER_ID:
                    findViewById(R.id.email_provider).setVisibility(View.VISIBLE);
                    break;
                default:
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Encountered unknown IDPProvider parcel with type: "
                                + providerParcel.getProviderType());
                    }
            }
        }

        LinearLayout btnHolder = (LinearLayout) findViewById(R.id.btn_holder);
        for (final IDPProvider provider: mIdpProviders) {
            View loginButton = null;
            switch (provider.getProviderId()) {
                case GoogleAuthProvider.PROVIDER_ID:
                    loginButton = getLayoutInflater()
                            .inflate(R.layout.idp_button_google, btnHolder, false);
                    break;
                case FacebookAuthProvider.PROVIDER_ID:
                    loginButton = getLayoutInflater()
                            .inflate(R.layout.idp_button_facebook, btnHolder, false);
                    break;
                default:
                    Log.e(TAG, "No button for provider " + provider.getProviderId());
            }
            if (loginButton != null) {
                loginButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mActivityHelper.showLoadingDialog(R.string.progress_dialog_loading);
                        provider.startLogin(AuthMethodPickerActivity.this);
                    }
                });
                provider.setAuthenticationCallback(this);
                btnHolder.addView(loginButton, 0);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_EMAIL_FLOW) {
            if (resultCode == RESULT_OK) {
                finish(RESULT_OK, new Intent());
            }
        } else if (requestCode == RC_SAVE_CREDENTIAL) {
            finish(RESULT_OK, new Intent());
        } else if (requestCode == RC_ACCOUNT_LINK) {
            finish(resultCode, new Intent());
        } else {
            for(IDPProvider provider : mIdpProviders) {
                provider.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onSuccess(final IDPResponse response) {
        AuthCredential credential = createCredential(response);
        final FirebaseAuth firebaseAuth = mActivityHelper.getFirebaseAuth();

        firebaseAuth
                .signInWithCredential(credential)
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Firebase sign in with credential unsuccessful"))
                .addOnCompleteListener(new CredentialSignInHandler(
                        AuthMethodPickerActivity.this,
                        mActivityHelper,
                        RC_ACCOUNT_LINK,
                        RC_SAVE_CREDENTIAL,
                        response));
    }

    @Override
    public void onFailure(Bundle extra) {
        // stay on this screen
        mActivityHelper.dismissDialog();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.email_provider) {
            Intent intent = EmailFlowUtil.createIntent(
                    this,
                    mActivityHelper.getFlowParams());
            startActivityForResult(intent, RC_EMAIL_FLOW);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIdpProviders != null) {
            for (final IDPProvider provider : mIdpProviders) {
                if (provider instanceof GoogleProvider) {
                    ((GoogleProvider) provider).disconnect();
                }
            }
        }
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams) {
        return ActivityHelper.createBaseIntent(context, AuthMethodPickerActivity.class, flowParams);
    }
}
