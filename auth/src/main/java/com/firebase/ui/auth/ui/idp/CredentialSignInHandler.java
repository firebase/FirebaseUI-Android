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

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.ui.auth.provider.IDPResponse;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.firebase.ui.auth.ui.account_link.WelcomeBackIDPPrompt;
import com.firebase.ui.auth.ui.account_link.WelcomeBackPasswordPrompt;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.ProviderQueryResult;

public class CredentialSignInHandler implements OnCompleteListener<AuthResult> {
    private final static String TAG = "CredentialSignInHandler";
    private int mAccountLinkResultCode;
    private int mSaveCredentialsResultCode;
    private Activity mActivity;
    private ActivityHelper mActivityHelper;
    private IDPResponse mResponse;

    public CredentialSignInHandler(
            Activity activity,
            ActivityHelper activityHelper,
            int accountLinkResultCode,
            int saveCredentialsResultCode,
            IDPResponse response) {
        mActivity = activity;
        mAccountLinkResultCode = accountLinkResultCode;
        mSaveCredentialsResultCode = saveCredentialsResultCode;
        mActivityHelper = activityHelper;
        mResponse = response;
    }

    @Override
    public void onComplete(@NonNull Task <AuthResult> task) {
        if (!task.isSuccessful()) {
            if (task.getException().getClass() ==
                    FirebaseAuthUserCollisionException.class) {
                final String email = mResponse.getEmail();
                FirebaseAuth firebaseAuth = mActivityHelper.getFirebaseAuth();
                firebaseAuth.fetchProvidersForEmail(email)
                        .addOnFailureListener(new TaskFailureLogger(
                                TAG, "Error fetching providers for email"))
                        .addOnSuccessListener(new StartWelcomeBackFlow(email));
            } else {
                mActivityHelper.dismissDialog();
                Log.e(
                        TAG,
                        "Unexpected exception when signing in with credential",
                        task.getException());
            }
        } else {
            FirebaseUser firebaseUser = task.getResult().getUser();
            String photoUrl = null;
            Uri photoUri = firebaseUser.getPhotoUrl();
            if (photoUri != null) {
                photoUrl = photoUri.toString();
            }
            mActivityHelper.dismissDialog();
            mActivity.startActivityForResult(SaveCredentialsActivity.createIntent(
                    mActivityHelper.getApplicationContext(),
                    mActivityHelper.getFlowParams(),
                    firebaseUser.getDisplayName(),
                    firebaseUser.getEmail(),
                    null,
                    mResponse.getProviderType(),
                    photoUrl
            ), mSaveCredentialsResultCode);
        }
    }

    private class StartWelcomeBackFlow implements OnSuccessListener<ProviderQueryResult> {
        private String mEmail;

        public StartWelcomeBackFlow(String email) {
            mEmail = email;
        }

        @Override
        public void onSuccess(@NonNull ProviderQueryResult result) {
            String provider = result.getProviders().get(0);
            if (provider.equals(EmailAuthProvider.PROVIDER_ID)) {
                mActivityHelper.dismissDialog();
                mActivity.startActivityForResult(
                        WelcomeBackPasswordPrompt.createIntent(
                                mActivityHelper.getApplicationContext(),
                                mActivityHelper.getFlowParams(),
                                mResponse
                        ), mAccountLinkResultCode);
    
            } else {
                mActivityHelper.dismissDialog();
                mActivity.startActivityForResult(
                        WelcomeBackIDPPrompt.createIntent(
                                mActivityHelper.getApplicationContext(),
                                mActivityHelper.getFlowParams(),
                                result.getProviders().get(0),
                                mResponse,
                                mEmail
                        ), mAccountLinkResultCode);
            }
        }
    }

}
