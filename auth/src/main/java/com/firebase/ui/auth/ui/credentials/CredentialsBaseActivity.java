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
import android.util.Log;

import com.firebase.ui.auth.api.CredentialsAPI;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.firebase.ui.auth.ui.NoControllerBaseActivity;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.ui.idp.IDPSignInContainerActivity;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;

public abstract class CredentialsBaseActivity extends NoControllerBaseActivity {
    private String TAG = "CredentialsBaseActivity";
    protected CredentialsAPI mCredentialsApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCredentialsApi = new CredentialsAPI(this, new CredentialsAPI.CallbackInterface() {
            @Override
            public void onAsyncTaskFinished() {
                asyncTasksDone();
            }
        });
    }

    /**
     * Override this method to handle async tasks. I.E.: if you need to wait until asyncTask(s)
     * will be done processing.
     */
    protected void asyncTasksDone() {
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCredentialsApi.isGoogleApiClient()){
            mCredentialsApi.getGoogleApiClient().connect();
        }
    }

    @Override
    protected void onStop() {
        if (mCredentialsApi.isGoogleApiClient()
                && mCredentialsApi.getGoogleApiClient().isConnected()) {
            mCredentialsApi.getGoogleApiClient().disconnect();
        }
        super.onStop();
    }

    protected void redirectToIdpSignIn(
            String email, String accountType, ArrayList<IDPProviderParcel> providers) {
        Intent nextIntent;
        switch (accountType) {
            case IdentityProviders.GOOGLE:
                nextIntent = IDPSignInContainerActivity.createIntent(
                        getApplicationContext(),
                        GoogleAuthProvider.PROVIDER_ID,
                        email,
                        providers,
                        mAppName);
                break;
            case IdentityProviders.FACEBOOK:
                nextIntent =
                        IDPSignInContainerActivity.createIntent(
                                getApplicationContext(),
                                FacebookAuthProvider.PROVIDER_ID,
                                email,
                                providers,
                                mAppName);
                break;
            default:
                Log.w(TAG, "unknown provider: " + accountType);
                nextIntent = AuthMethodPickerActivity.createIntent(
                        getApplicationContext(),
                        mAppName,
                        providers
                );
        }
        startActivity(nextIntent);
    }
}
