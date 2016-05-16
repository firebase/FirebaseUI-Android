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
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.IDPProvider;
import com.firebase.ui.auth.provider.IDPProviderParcel;
import com.firebase.ui.auth.provider.IDPResponse;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.account_link.AccountLinkInitActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.List;

public class IDPSignInContainerActivity extends IDPBaseActivity implements IDPProvider.IDPCallback {
    private static final String TAG = "IDPSignInContainer";
    private static final String PROVIDER = "sign_in_provider";
    private static final String EMAIL = "email";
    private static final int RC_ACCOUNT_LINK = 3;
    private IDPProvider mIDPProvider;
    private String mProvider;
    private String mEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProvider = getIntent().getStringExtra(PROVIDER);
        mEmail = getIntent().getStringExtra(EMAIL);
        IDPProviderParcel providerParcel = null;
        for (IDPProviderParcel parcel : mActivityHelper.flowParams.providerInfo) {
            if (parcel.getProviderType().equalsIgnoreCase(mProvider)) {
                providerParcel = parcel;
                break;
            }
        }
        if (providerParcel == null) {
            // we don't have a provider to handle this
            finish(RESULT_CANCELED, new Intent());
            return;
        }
        if (mProvider.equalsIgnoreCase(FacebookAuthProvider.PROVIDER_ID)) {
            mIDPProvider = new FacebookProvider(this, providerParcel);
        } else if (mProvider.equalsIgnoreCase(GoogleAuthProvider.PROVIDER_ID)) {
            mIDPProvider = new GoogleProvider(this, providerParcel);
        }
        mIDPProvider.setAuthenticationCallback(this);
        mIDPProvider.startLogin(this, mEmail);
    }

    private void startAccountLinkingActivity(FirebaseUser firebaseUser, IDPResponse response) {
        List<String> providers = firebaseUser.getProviders();
        String provider = null;
        if (providers.isEmpty()) {
            Log.e(TAG, "User has no existing providers to link with" );
        } else {
            provider = firebaseUser.getProviders().get(0);
        }
        startActivityForResult(AccountLinkInitActivity.createIntent(
                this,
                mActivityHelper.flowParams,
                firebaseUser.getEmail(),
                null,
                response,
                provider),
                RC_ACCOUNT_LINK);
    }

    @Override
    public void onSuccess(final IDPResponse response) {
        Intent data = new Intent();
        data.putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, response);
        AuthCredential credential = createCredential(response);
        FirebaseAuth firebaseAuth = mActivityHelper.getFirebaseAuth();
        Task<AuthResult> authResultTask = firebaseAuth.signInWithCredential(credential);
        authResultTask
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        startAccountLinkingActivity(task.getResult().getUser(), response);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failure authenticating with credential");
                    }
                });
    }

    @Override
    public void onFailure(Bundle extra) {
        finish(RESULT_CANCELED, new Intent());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_ACCOUNT_LINK) {
            finish(resultCode, new Intent());
        } else {
            mIDPProvider.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            String provider,
            String email) {
        return ActivityHelper.createBaseIntent(
                context,
                IDPSignInContainerActivity.class,
                flowParams)
                .putExtra(PROVIDER, provider)
                .putExtra(EMAIL, email);
    }
}
