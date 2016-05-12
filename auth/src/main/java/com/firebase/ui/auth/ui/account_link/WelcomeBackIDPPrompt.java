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
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.idp.provider.FacebookProvider;
import com.firebase.ui.auth.choreographer.idp.provider.GoogleProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.firebase.ui.auth.choreographer.idp.provider.IDPResponse;
import com.firebase.ui.auth.ui.NoControllerBaseActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;

public class WelcomeBackIDPPrompt extends NoControllerBaseActivity
        implements View.OnClickListener, IDPProvider.IDPCallback {

    private static final String TAG = "WelcomeBackIDPPrompt";

    private IDPProvider mIDPProvider;
    private String mProviderId;
    private IDPResponse mIdpResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getResources().getString(R.string.sign_in));
        mProviderId = getProviderIdFromIntent();
        mIdpResponse = getIntent().getParcelableExtra(ControllerConstants.EXTRA_IDP_RESPONSE);
        setContentView(R.layout.welcome_back_idp_prompt_layout);

        mIDPProvider = null;
        for (IDPProviderParcel providerParcel: mProviderParcels) {
            if (mProviderId.equals(providerParcel.getProviderType())) {
                switch (mProviderId) {
                    case GoogleAuthProvider.PROVIDER_ID:
                        mIDPProvider = new GoogleProvider(this, providerParcel);
                        break;
                    case FacebookAuthProvider.PROVIDER_ID:
                        mIDPProvider = new FacebookProvider(this, providerParcel);
                        break;
                    default:
                        Log.w(TAG, "Unknown provider: " + mProviderId);
                        finish(RESULT_CANCELED, getIntent());
                        return;
                }
            }
        }

        if (mIDPProvider == null) {
            getIntent().putExtra(
                    ControllerConstants.EXTRA_ERROR_MESSAGE,
                    "Firebase login successful. Account linking failed due to provider not "
                            + "enabled by application");
            finish(RESULT_CANCELED, getIntent());
        }

        ((TextView) findViewById(R.id.welcome_back_idp_prompt))
                .setText(getIdpPromptString(getEmailFromIntent(), getAppNameFromIntent()));

        mIDPProvider.setAuthenticationCallback(this);
        findViewById(R.id.welcome_back_idp_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIDPProvider.startLogin(WelcomeBackIDPPrompt.this, getEmailFromIntent());
            }
        });
    }

    private String getIdpPromptString(String email, String appName) {
        String promptStringTemplate = getResources().getString(R.string.welcome_back_idp_prompt);
        return String.format(promptStringTemplate, email, appName, mIDPProvider.getName(this));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mIDPProvider.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        next(mIdpResponse, mProviderId);
    }

    @Override
    public void onSuccess(IDPResponse idpResponse) {
        Intent data = getIntent();
        data.putExtra(ControllerConstants.EXTRA_PROVIDER, mProviderId);
        next(idpResponse, mProviderId);
    }

    @Override
    public void onFailure(Bundle extra) {
        Toast.makeText(getApplicationContext(), "Error signing in", Toast.LENGTH_LONG).show();
        next(mIdpResponse, mProviderId);
    }

    private String getAppNameFromIntent() {
        return getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);
    }

    private String getProviderIdFromIntent() {
        return getIntent().getStringExtra(ControllerConstants.EXTRA_PROVIDER);
    }

    private String getEmailFromIntent() {
        return getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);
    }

    public static Intent createIntent(
            Context context,
            String providerId,
            ArrayList<IDPProviderParcel> providers,
            String appName,
            String email) {
        return new Intent().setClass(context, WelcomeBackIDPPrompt.class)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
                .putExtra(ControllerConstants.EXTRA_PROVIDER, providerId)
                .putExtra(ControllerConstants.EXTRA_PROVIDERS, providers)
                .putExtra(ControllerConstants.EXTRA_EMAIL, email);
    }

    private void next(IDPResponse idpResponse, String provider) {
        if (idpResponse == null) {
            return; // do nothing
        }
        AuthCredential credential;
        switch (provider) {
            case GoogleAuthProvider.PROVIDER_ID:
                credential = GoogleProvider.createAuthCredential(idpResponse);
                break;
            case FacebookAuthProvider.PROVIDER_ID:
                credential = FacebookProvider.createAuthCredential(idpResponse);
                break;
            default:
                Log.e(TAG, "Unknown provider: " + provider);
                finish(Activity.RESULT_FIRST_USER, new Intent());
                return;
        }
        if (credential == null) {
            Log.e(TAG, "No credential returned");
            finish(Activity.RESULT_FIRST_USER, new Intent());
            return;
        }

        FirebaseAuth firebaseAuth = getFirebaseAuth();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Task<AuthResult> authResultTask = firebaseAuth.signInWithCredential(credential);
            authResultTask.addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    finish(Activity.RESULT_OK, new Intent());
                }
            });

        } else {
            Task<AuthResult> authResultTask = currentUser.linkWithCredential(credential);
            authResultTask.addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    finish(Activity.RESULT_OK, new Intent());
                }
            });
        }
    }
}
