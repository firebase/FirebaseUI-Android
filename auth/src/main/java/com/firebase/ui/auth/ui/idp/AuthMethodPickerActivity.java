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
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.IDPProvider;
import com.firebase.ui.auth.provider.IDPProviderParcel;
import com.firebase.ui.auth.provider.IDPResponse;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.firebase.ui.auth.ui.account_link.WelcomeBackIDPPrompt;
import com.firebase.ui.auth.ui.account_link.WelcomeBackPasswordPrompt;
import com.firebase.ui.auth.ui.email.EmailHintContainerActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Presents the list of authentication options for this app to the user.
 */
public class AuthMethodPickerActivity
        extends IDPBaseActivity
        implements IDPProvider.IDPCallback, View.OnClickListener {

    private static final int RC_EMAIL_FLOW = 2;
    private static final int RC_WELCOME_BACK_IDP = 3;
    private static final int RC_ACCOUNT_LINK = 4;
    private static final String TAG = "AuthMethodPicker";
    private ArrayList<IDPProvider> mIdpProviders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_method_picker_layout);
        Button emailButton = (Button) findViewById(R.id.email_provider);
        emailButton.setOnClickListener(this);
        populateIdpList(mActivityHelper.flowParams.providerInfo);
    }

    private void populateIdpList(List<IDPProviderParcel> providers) {
        mIdpProviders = new ArrayList<>();
        for (IDPProviderParcel providerParcel : providers) {
            switch (providerParcel.getProviderType()) {
                case FacebookAuthProvider.PROVIDER_ID :
                    mIdpProviders.add(new FacebookProvider(this, providerParcel));
                    break;
                case GoogleAuthProvider.PROVIDER_ID:
                    mIdpProviders.add(new GoogleProvider(this, providerParcel));
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
                        provider.startLogin(AuthMethodPickerActivity.this, null);
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
        } else if (requestCode == RC_ACCOUNT_LINK) {
            finish(RESULT_OK, new Intent());
        } else if (requestCode == RC_WELCOME_BACK_IDP) {
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

            firebaseAuth.signInWithCredential(credential).addOnCompleteListener(
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            if (task.getException().getClass() ==
                                    FirebaseAuthUserCollisionException.class) {
                                final String email = response.getEmail();
                                firebaseAuth.fetchProvidersForEmail(email)
                                        .addOnCompleteListener(
                                                new OnCompleteListener<ProviderQueryResult>() {
                                    @Override
                                    public void onComplete(
                                            @NonNull Task<ProviderQueryResult> task) {
                                        String provider = task.getResult().getProviders().get(0);
                                        if (provider.equals(EmailAuthProvider.PROVIDER_ID)) {
                                            mActivityHelper.dismissDialog();
                                            startActivityForResult(
                                                    WelcomeBackPasswordPrompt.createIntent(
                                                            mActivityHelper.getApplicationContext(),
                                                            mActivityHelper.flowParams,
                                                            response
                                                    ), RC_WELCOME_BACK_IDP);

                                        } else {
                                            mActivityHelper.dismissDialog();
                                            startActivityForResult(
                                                    WelcomeBackIDPPrompt.createIntent(
                                                        mActivityHelper.getApplicationContext(),
                                                        mActivityHelper.flowParams,
                                                        task.getResult().getProviders().get(0),
                                                        response,
                                                        email
                                            ), RC_WELCOME_BACK_IDP);
                                        }
                                    }
                                });
                            }
                        } else {
                            FirebaseUser firebaseUser = task.getResult().getUser();
                            String photoUrl = null;
                            Uri photoUri = firebaseUser.getPhotoUrl();
                            if (photoUri != null) {
                                photoUrl = photoUri.toString();
                            }
                            mActivityHelper.dismissDialog();
                            startActivityForResult(SaveCredentialsActivity.createIntent(
                                    mActivityHelper.getApplicationContext(),
                                    mActivityHelper.flowParams,
                                    firebaseUser.getDisplayName(),
                                    firebaseUser.getEmail(),
                                    null,
                                    firebaseUser.getProviderId(),
                                    photoUrl
                            ), RC_ACCOUNT_LINK);
                        }
                    }
                }
        ).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Firebase login unsuccessful");
            }
        });
    }

    @Override
    public void onFailure(Bundle extra) {
        // stay on this screen
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.email_provider) {
            Intent intent = EmailHintContainerActivity.createIntent(
                    this,
                    mActivityHelper.flowParams);
            startActivityForResult(intent, RC_EMAIL_FLOW);
        }
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams) {
        return ActivityHelper.createBaseIntent(context, AuthMethodPickerActivity.class, flowParams);
    }
}
