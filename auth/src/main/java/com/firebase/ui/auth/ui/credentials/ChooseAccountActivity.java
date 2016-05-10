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

package com.firebase.ui.auth.ui.credentials;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public class ChooseAccountActivity extends CredentialsBaseActivity {
    private static final String TAG = "ChooseAccountActivity";
    private static final int RC_CREDENTIALS_READ = 2;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
    }

    @Override
    public void asyncTasksDone() {
        mCredentialsApi.resolveSavedEmails(this);
    }

    private void logInWithCredential(final String email, final String password, final String accountType) {
        if (email != null
                && mCredentialsApi.isCredentialsAvailable()
                && !mCredentialsApi.isSignInResolutionNeeded()) {
            if (password != null && !password.isEmpty()) {
                // email/password combination
                getFirebaseAuth().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                finish(RESULT_OK, new Intent());
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Throwable throwable) {
                                redirectToIdpSignIn(email, accountType, mProviderParcels);
                                finish(RESULT_OK, new Intent());
                            }
                        });
            } else {
                // identifier/provider combination
                redirectToIdpSignIn(email, accountType, mProviderParcels);
                finish(RESULT_OK, new Intent());
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
        }

        if (requestCode == RC_CREDENTIALS_READ) {
            if (resultCode == RESULT_OK) {
                // credential selected from SmartLock, log in with that credential
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                mCredentialsApi.handleCredential(credential);
                mCredentialsApi.resolveSignIn();
                logInWithCredential(
                        mCredentialsApi.getEmailFromCredential(),
                        mCredentialsApi.getPasswordFromCredential(),
                        mCredentialsApi.getAccountTypeFromCredential()
                );
            } else if (resultCode == RESULT_CANCELED) {
                // Smart lock selector cancelled, go to the AuthMethodPicker screen
                startActivity(AuthMethodPickerActivity.createIntent(
                        getApplicationContext(),
                        mAppName,
                        mProviderParcels
                ));
                finish(RESULT_OK, new Intent());
            } else if (resultCode == RESULT_FIRST_USER) {
                // TODO: (serikb) figure out flow
            }
        }
    }

}
