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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;

import java.util.ArrayList;

public class CredentialsInitActivity extends CredentialsBaseActivity {
    private static final String TAG = "CredentialsInitActivity";

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        try{
            FirebaseApp.getInstance(mAppName);
            return;
        } catch (IllegalStateException e) {
            Log.d(TAG, "FirebaseApp is not created yet");
        }

        String apiaryKey = getIntent().getStringExtra(ControllerConstants.EXTRA_APIARY_KEY);
        String applicationId = getIntent().getStringExtra(ControllerConstants.EXTRA_APPLICATION_ID);
        String appName = getIntent().getStringExtra(ControllerConstants.EXTRA_APP_NAME);

        FirebaseOptions options
                = new FirebaseOptions.Builder()
                .setApiKey(apiaryKey)
                .setApplicationId(applicationId)
                .build();

        FirebaseApp.initializeApp(this, options, appName);
    }

    public static Intent createIntent(
            Context context, String appName, ArrayList<IDPProviderParcel> parcels, String
            apiaryKey, String applicationId, String termsOfServiceUrl, int theme) {
        return new Intent()
                .setClass(context, CredentialsInitActivity.class)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
                .putExtra(ControllerConstants.EXTRA_APIARY_KEY, apiaryKey)
                .putExtra(ControllerConstants.EXTRA_APPLICATION_ID, applicationId)
                .putExtra(ControllerConstants.EXTRA_TERMS_OF_SERVICE_URL, termsOfServiceUrl)
                .putParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS, parcels)
                .putExtra(ControllerConstants.EXTRA_THEME, theme);
    }

    public void next(String password, final String email, final String accountType) {
        if (mCredentialsApi.isPlayServicesAvailable()
                && mCredentialsApi.isCredentialsAvailable()) {
            if (mCredentialsApi.isAutoSignInAvailable()) {
                mCredentialsApi.googleSilentSignIn();
                // TODO: (serikb) authenticate Firebase user and continue to application
                if (password != null && !password.isEmpty()) {
                    // login with username/password
                    getFirebaseAuth().signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    finish(Activity.RESULT_OK, new Intent());
                                }
                            });
                } else {
                    // log in with id/provider
                    redirectToIdpSignIn(email, accountType, mProviderParcels);
                }
            } else if (mCredentialsApi.isSignInResolutionNeeded()) {
                // resolve credential
                startActivity(new Intent(getApplicationContext(), ChooseAccountActivity.class));
                finish();
            }
        }

    }

    @Override
    protected void asyncTasksDone() {
        String email = mCredentialsApi.getEmailFromCredential();
        String password = mCredentialsApi.getPasswordFromCredential();
        String accountType = mCredentialsApi.getAccountTypeFromCredential();
        next(email, password, accountType);
    }
}
