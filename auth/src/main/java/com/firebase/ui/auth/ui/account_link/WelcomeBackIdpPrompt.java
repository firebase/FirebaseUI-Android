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

package com.firebase.ui.auth.ui.account_link;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.provider.AuthCredentialHelper;
import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.IdpProvider;
import com.firebase.ui.auth.provider.IdpProvider.IdpCallback;
import com.firebase.ui.auth.provider.TwitterProvider;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

public class WelcomeBackIdpPrompt extends AppCompatBase
        implements View.OnClickListener, IdpCallback {

    private static final String TAG = "WelcomeBackIDPPrompt";
    private IdpProvider mIdpProvider;
    private IdpResponse mPrevIdpResponse;
    private AuthCredential mPrevCredential;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String providerId = getProviderIdFromIntent();
        mPrevIdpResponse = getIntent().getParcelableExtra(ExtraConstants.EXTRA_IDP_RESPONSE);
        setContentView(R.layout.welcome_back_idp_prompt_layout);

        mIdpProvider = null;
        for (IdpConfig idpConfig : mActivityHelper.getFlowParams().providerInfo) {
            if (providerId.equals(idpConfig.getProviderId())) {
                switch (providerId) {
                    case GoogleAuthProvider.PROVIDER_ID:
                        mIdpProvider = new GoogleProvider(this, idpConfig, getEmailFromIntent());
                        break;
                    case FacebookAuthProvider.PROVIDER_ID:
                        mIdpProvider = new FacebookProvider(this, idpConfig);
                        break;
                    case TwitterAuthProvider.PROVIDER_ID:
                        mIdpProvider = new TwitterProvider(this);
                        break;
                    default:
                        Log.w(TAG, "Unknown provider: " + providerId);
                        finish(RESULT_CANCELED, getIntent());
                        return;
                }
            }
        }

        if (mPrevIdpResponse != null) {
            mPrevCredential = AuthCredentialHelper.getAuthCredential(mPrevIdpResponse);
        }

        if (mIdpProvider == null) {
            getIntent().putExtra(
                    ExtraConstants.EXTRA_ERROR_MESSAGE,
                    "Firebase login successful. Account linking failed due to provider not enabled by application");
            finish(RESULT_CANCELED, getIntent());
            return;
        }

        ((TextView) findViewById(R.id.welcome_back_idp_prompt))
                .setText(getIdpPromptString(getEmailFromIntent()));

        mIdpProvider.setAuthenticationCallback(this);
        findViewById(R.id.welcome_back_idp_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivityHelper.showLoadingDialog(R.string.progress_dialog_signing_in);
                mIdpProvider.startLogin(WelcomeBackIdpPrompt.this);
            }
        });
    }

    private String getIdpPromptString(String email) {
        String promptStringTemplate = getResources().getString(R.string.welcome_back_idp_prompt);
        return String.format(promptStringTemplate, email, mIdpProvider.getName(this));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mIdpProvider.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        next(mPrevIdpResponse);
    }

    @Override
    public void onSuccess(IdpResponse idpResponse) {
        next(idpResponse);
    }

    @Override
    public void onFailure(Bundle extra) {
        Toast.makeText(getApplicationContext(), "Error signing in", Toast.LENGTH_LONG).show();
        finish(RESULT_CANCELED, new Intent());
    }

    private String getProviderIdFromIntent() {
        return getIntent().getStringExtra(ExtraConstants.EXTRA_PROVIDER);
    }

    private String getEmailFromIntent() {
        return getIntent().getStringExtra(ExtraConstants.EXTRA_EMAIL);
    }

    private void next(final IdpResponse newIdpResponse) {
        if (newIdpResponse == null) {
            return; // do nothing
        }

        AuthCredential newCredential = AuthCredentialHelper.getAuthCredential(newIdpResponse);
        if (newCredential == null) {
            Log.e(TAG, "No credential returned");
            finish(Activity.RESULT_FIRST_USER, new Intent());
            return;
        }

        final FirebaseAuth firebaseAuth = mActivityHelper.getFirebaseAuth();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Task<AuthResult> authResultTask = firebaseAuth.signInWithCredential(newCredential);
            authResultTask.addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful() && mPrevCredential != null) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        firebaseUser.linkWithCredential(mPrevCredential);
                        firebaseAuth.signOut();
                        firebaseAuth
                                .signInWithCredential(mPrevCredential)
                                .addOnFailureListener(new TaskFailureLogger(
                                        TAG, "Error signing in with previous credential"))
                                .addOnCompleteListener(new FinishListener(newIdpResponse));
                    } else {
                        finish(Activity.RESULT_OK, new Intent().putExtra(
                                ExtraConstants.EXTRA_IDP_RESPONSE, newIdpResponse));
                    }
                }
            }).addOnFailureListener(
                    new TaskFailureLogger(TAG, "Error signing in with new credential"));
        } else {
            Task<AuthResult> authResultTask = currentUser.linkWithCredential(newCredential);
            authResultTask
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error linking with credential"))
                    .addOnCompleteListener(new FinishListener(newIdpResponse));
        }
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            String providerId,
            IdpResponse idpResponse,
            String email) {
        return BaseHelper.createBaseIntent(context, WelcomeBackIdpPrompt.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_PROVIDER, providerId)
                .putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, idpResponse)
                .putExtra(ExtraConstants.EXTRA_EMAIL, email);
    }

    private class FinishListener implements OnCompleteListener<AuthResult> {
        private final IdpResponse mIdpResponse;

        FinishListener(IdpResponse idpResponse) {
            mIdpResponse = idpResponse;
        }

        public void onComplete(@NonNull Task task) {
            finish(Activity.RESULT_OK,
                   new Intent().putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, mIdpResponse));
        }
    }
}
