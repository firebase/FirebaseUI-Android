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

package com.firebase.ui.auth.api;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebase.ui.auth.BuildConfig;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.ProviderQueryResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HeadlessAPIWrapperImpl implements HeadlessAPIWrapper, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "HeadlessAPIWrapperImpl";
    private final FirebaseAuth mAuthObject;

    private long apiTimeOut = 30000L;

    public HeadlessAPIWrapperImpl(FirebaseAuth authObject) {
        if(authObject == null) {
            throw new IllegalArgumentException();
        }
        this.mAuthObject = authObject;
    }

    @Override
    public boolean isAccountExists(final String emailAddress) {
        Task<ProviderQueryResult> curTask = mAuthObject.fetchProvidersForEmail(emailAddress);
        ProviderQueryResult mProviderQueryResult = getResultSync(curTask);

        if (mProviderQueryResult == null || mProviderQueryResult.getProviders() == null
                    || mProviderQueryResult.getProviders().size() == 0) {
                return false;
        }
        return true;
    }


    @Override
    public List<String> getProviderList(String emailAddress) {
        if (emailAddress == null) {
            return new ArrayList<>();
        }
        Task<ProviderQueryResult> curTask = mAuthObject.fetchProvidersForEmail(emailAddress);
        ProviderQueryResult mProviderQueryResult = getResultSync(curTask);

        if (mProviderQueryResult == null || mProviderQueryResult.getProviders() == null
                || mProviderQueryResult.getProviders().size() == 0) {
            return new ArrayList<>();
        }
        return mProviderQueryResult.getProviders();
    }

    @Override
    public boolean resetEmailPassword(String emailAddress) {
        Task<Void> curTask = mAuthObject.sendPasswordResetEmail(emailAddress);
        try {
            Tasks.await(curTask);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public FirebaseUser signInWithEmailPassword(String emailAddress, String password) {
        Task<AuthResult> curTask = mAuthObject.signInWithEmailAndPassword(emailAddress, password);
        AuthResult authResult= getResultSync(curTask);
        if (authResult == null) {
            return null;
        }

        return authResult.getUser();
    }

    @Override
    public FirebaseUser signInWithCredential(AuthCredential credential) {
        Task<AuthResult> curTask = mAuthObject.signInWithCredential(credential);
        AuthResult authResult = getResultSync(curTask);
        if (authResult == null) {
            return null;
        }
        return authResult.getUser();
    }

    @Override
    public FirebaseUser getCurrentUser() {
        return mAuthObject.getCurrentUser();
    }

    @Override
    public FirebaseUser createEmailWithPassword(String emailAddress, String password) {
        Task<AuthResult> curTask
                = mAuthObject.createUserWithEmailAndPassword(emailAddress, password);

        AuthResult authResult = getResultSync(curTask);

        if (authResult == null) {
            return null;
        }
        return authResult.getUser();
    }

    @Override
    public boolean isGMSCorePresent(Context context) {
        return isGMSCorePresent(context, GoogleApiAvailability.getInstance());
    }

    protected boolean isGMSCorePresent(Context context, GoogleApiAvailability apiAvailability) {
        int result = apiAvailability.isGooglePlayServicesAvailable(context);
        if ( result == ConnectionResult.SUCCESS
                || result == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ) {
            return true;
        }
        return false;
    }

    @Override
    public PendingIntent getEmailHints(Context context) {
        if (!isGMSCorePresent(context,GoogleApiAvailability.getInstance())) {
            return null;
        }

        GoogleApiClient mCredentialsClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addApi(Auth.CREDENTIALS_API)
                .build();

        mCredentialsClient.connect();

        HintRequest hintRequest = new HintRequest.Builder()
                .setHintPickerConfig(new CredentialPickerConfig.Builder()
                        .setShowCancelButton(true)
                        .build())
                .setEmailAddressIdentifierSupported(true)
                .build();

        PendingIntent intent =
                Auth.CredentialsApi.getHintPickerIntent(mCredentialsClient, hintRequest);

        return intent;
    }

    @Override
    public void setTimeOut(long milliseconds) {
        this.apiTimeOut = milliseconds;
    }

    @Override
    public void signOut(Context ctx) {
        if (mAuthObject == null) {
            return;
        }
        mAuthObject.signOut();
    }

    @Override
    public FirebaseUser linkWithCredential(FirebaseUser user, AuthCredential credential) throws ExecutionException {
        Task<AuthResult> result = user.linkWithCredential(credential);
        try {
            Tasks.await(result, apiTimeOut, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace();
            return null;
        }
        return result.getResult().getUser();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private <T> T getResultSync(Task<T> curTask) {
        try {
            Tasks.await(curTask, apiTimeOut, TimeUnit.MILLISECONDS);
            return curTask.getResult();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "API request exception: " + e.getMessage());
            }
            return null;
        }
    }
}
