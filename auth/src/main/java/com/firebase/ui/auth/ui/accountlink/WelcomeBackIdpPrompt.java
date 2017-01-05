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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ResultCodes;
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
import com.firebase.ui.auth.ui.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WelcomeBackIdpPrompt extends AppCompatBase implements IdpCallback {
    private static final String TAG = "WelcomeBackIdpPrompt";

    private IdpProvider mIdpProvider;
    private AuthCredential mPrevCredential;

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            User existingUser,
            IdpResponse newUserResponse) {
        return BaseHelper.createBaseIntent(context, WelcomeBackIdpPrompt.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_USER, existingUser)
                .putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, newUserResponse);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_back_idp_prompt_layout);

        IdpResponse newUserIdpResponse = IdpResponse.fromResultIntent(getIntent());
        mPrevCredential = AuthCredentialHelper.getAuthCredential(newUserIdpResponse);

        User oldUser = User.getUser(getIntent());

        String providerId = oldUser.getProvider();
        for (IdpConfig idpConfig : mActivityHelper.getFlowParams().providerInfo) {
            if (providerId.equals(idpConfig.getProviderId())) {
                switch (providerId) {
                    case GoogleAuthProvider.PROVIDER_ID:
                        mIdpProvider = new GoogleProvider(this, idpConfig, oldUser.getEmail());
                        break;
                    case FacebookAuthProvider.PROVIDER_ID:
                        mIdpProvider = new FacebookProvider(
                                this, idpConfig, mActivityHelper.getFlowParams().themeId);
                        break;
                    case TwitterAuthProvider.PROVIDER_ID:
                        mIdpProvider = new TwitterProvider(this);
                        break;
                    default:
                        Log.w(TAG, "Unknown provider: " + providerId);
                        finish(ResultCodes.CANCELED,
                               IdpResponse.getErrorCodeIntent(ErrorCodes.UNKNOWN_ERROR));
                        return;
                }
            }
        }

        if (mIdpProvider == null) {
            Log.w(TAG, "Firebase login unsuccessful."
                    + " Account linking failed due to provider not enabled by application: "
                    + providerId);
            finish(ResultCodes.CANCELED, IdpResponse.getErrorCodeIntent(ErrorCodes.UNKNOWN_ERROR));
            return;
        }

        ((TextView) findViewById(R.id.welcome_back_idp_prompt))
                .setText(getIdpPromptString(oldUser.getEmail()));

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
        return getString(R.string.welcome_back_idp_prompt, email, mIdpProvider.getName(this));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mIdpProvider.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSuccess(final IdpResponse idpResponse) {
        if (idpResponse == null) {
            return; // do nothing
        }

        AuthCredential newCredential = AuthCredentialHelper.getAuthCredential(idpResponse);
        if (newCredential == null) {
            Log.e(TAG, "No credential returned");
            finish(ResultCodes.CANCELED, IdpResponse.getErrorCodeIntent(ErrorCodes.UNKNOWN_ERROR));
            return;
        }

        FirebaseUser currentUser = mActivityHelper.getCurrentUser();
        if (currentUser == null) {
            mActivityHelper.getFirebaseAuth()
                    .signInWithCredential(newCredential)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult result) {
                            if (mPrevCredential != null) {
                                result.getUser()
                                        .linkWithCredential(mPrevCredential)
                                        .addOnFailureListener(new TaskFailureLogger(
                                                TAG, "Error signing in with previous credential"))
                                        .addOnCompleteListener(new FinishListener(idpResponse));
                            } else {
                                finish(ResultCodes.OK, IdpResponse.getIntent(idpResponse));
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            finishWithError();
                        }
                    })
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error signing in with new credential"));
        } else {
            currentUser
                    .linkWithCredential(newCredential)
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error linking with credential"))
                    .addOnCompleteListener(new FinishListener(idpResponse));
        }
    }

    @Override
    public void onFailure(Bundle extra) {
        finishWithError();
    }

    private void finishWithError() {
        Toast.makeText(this, R.string.general_error, Toast.LENGTH_LONG).show();
        finish(ResultCodes.CANCELED, IdpResponse.getErrorCodeIntent(ErrorCodes.UNKNOWN_ERROR));
    }

    private class FinishListener implements OnCompleteListener<AuthResult> {
        private final IdpResponse mIdpResponse;

        FinishListener(IdpResponse idpResponse) {
            mIdpResponse = idpResponse;
        }

        public void onComplete(@NonNull Task task) {
            finish(ResultCodes.OK, IdpResponse.getIntent(mIdpResponse));
        }
    }
}
