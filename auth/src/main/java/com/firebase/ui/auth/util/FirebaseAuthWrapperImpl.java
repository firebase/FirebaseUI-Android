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

package com.firebase.ui.auth.util;

import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.firebase.ui.auth.BuildConfig;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.ProviderQueryResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FirebaseAuthWrapperImpl
        implements FirebaseAuthWrapper, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "FirebaseAuthWrapperImpl";

    private static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(2);

    private final FirebaseAuth mFirebaseAuth;

    private long mTimeoutMs = DEFAULT_TIMEOUT;

    public FirebaseAuthWrapperImpl(@NonNull FirebaseAuth firebaseAuth) {
        if (firebaseAuth == null) {
            throw new IllegalArgumentException("firebaseAuth must not be null");
        }
        this.mFirebaseAuth = firebaseAuth;
    }

    @Override
    @WorkerThread
    public boolean isExistingAccount(@Nullable final String email) {
        if (email == null) {
            return false;
        }
        return hasProviders(await(mFirebaseAuth.fetchProvidersForEmail(email)));
    }

    @Override
    @WorkerThread
    @NonNull
    public List<String> getProvidersForEmail(@Nullable String emailAddress) {
        if (emailAddress == null) {
            return Collections.emptyList();
        }

        ProviderQueryResult result =
                await(mFirebaseAuth.fetchProvidersForEmail(emailAddress));
        if (hasProviders(result)) {
            return result.getProviders();
        }

        return Collections.emptyList();
    }

    @Override
    @WorkerThread
    public boolean resetPasswordForEmail(@NonNull String emailAddress) {
        Task<Void> curTask = mFirebaseAuth.sendPasswordResetEmail(emailAddress);
        try {
            Tasks.await(curTask);
        } catch (ExecutionException | InterruptedException e) {
            Log.w(TAG, "attempt to reset password failed", e);
            return false;
        }
        return true;
    }

    @Override
    @WorkerThread
    @Nullable
    public FirebaseUser signInWithEmailPassword(
            @NonNull String emailAddress,
            @NonNull String password) {
        AuthResult authResult =
                await(mFirebaseAuth.signInWithEmailAndPassword(emailAddress, password));
        return authResult == null ? null : authResult.getUser();
    }

    @Override
    @WorkerThread
    @Nullable
    public FirebaseUser signInWithCredential(@NonNull AuthCredential credential) {
        Task<AuthResult> curTask = mFirebaseAuth.signInWithCredential(credential);
        AuthResult authResult = await(curTask);
        return authResult == null ? null : authResult.getUser();
    }

    @Override
    @WorkerThread
    @Nullable
    public FirebaseUser getCurrentUser() {
        return mFirebaseAuth.getCurrentUser();
    }

    @Override
    @WorkerThread
    @Nullable
    public FirebaseUser createUserWithEmailAndPassword(
            @NonNull String emailAddress,
            @NonNull String password) throws ExecutionException, InterruptedException {
        Task<AuthResult> curTask;
        curTask = mFirebaseAuth.createUserWithEmailAndPassword(emailAddress, password);
        Tasks.await(curTask);
        return curTask.getResult().getUser();
    }

    @Override
    public boolean isPlayServicesAvailable(Context context) {
        return isPlayServicesAvailable(context, GoogleApiAvailability.getInstance());
    }

    protected boolean isPlayServicesAvailable(
            Context context,
            GoogleApiAvailability apiAvailability) {
        int result = apiAvailability.isGooglePlayServicesAvailable(context);
        return result == ConnectionResult.SUCCESS
                || result == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;
    }

    @Override
    public PendingIntent getEmailHintIntent(FragmentActivity fragmentActivity) {
        if (!isPlayServicesAvailable(fragmentActivity, GoogleApiAvailability.getInstance())) {
            return null;
        }

        GoogleApiClient client = new GoogleApiClient.Builder(fragmentActivity)
                .addConnectionCallbacks(this)
                .addApi(Auth.CREDENTIALS_API)
                .enableAutoManage(fragmentActivity, new OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.e(TAG,
                            "Client connection failed: " + connectionResult.getErrorMessage());
                    }
                })
                .build();

        HintRequest hintRequest = new HintRequest.Builder()
                .setHintPickerConfig(new CredentialPickerConfig.Builder()
                        .setShowCancelButton(true)
                        .build())
                .setEmailAddressIdentifierSupported(true)
                .build();

        return Auth.CredentialsApi.getHintPickerIntent(client, hintRequest);
    }

    @Override
    public void setTimeOut(long timeoutMs) {
        this.mTimeoutMs = timeoutMs;
    }

    @Override
    @WorkerThread
    @Nullable
    public FirebaseUser linkWithCredential(
            @NonNull FirebaseUser user,
            @NonNull AuthCredential credential)
            throws ExecutionException {
        AuthResult linkResult = await(user.linkWithCredential(credential));
        return linkResult == null ? null : linkResult.getUser();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {}

    @Override
    public void onConnectionSuspended(int cause) {}

    private <T> T await(@NonNull Task<T> curTask) {
        try {
            Tasks.await(curTask, mTimeoutMs, TimeUnit.MILLISECONDS);
            return curTask.getResult();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "API request dispatch failed", e);
            }
            return null;
        }
    }

    private boolean hasProviders(@Nullable ProviderQueryResult result) {
        return result != null
                && result.getProviders() != null
                && result.getProviders().size() > 0;
    }
}
