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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.net.ConnectException;

/**
 * A {@link Task} based wrapper for the Smart Lock for Passwords API.
 */
public class CredentialsApiHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient mClient;
    private TaskCompletionSource<Bundle> mGoogleApiConnectionTask = new TaskCompletionSource<>();

    private CredentialsApiHelper() {
    }

    public static CredentialsApiHelper getInstance(FragmentActivity activity) {
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(activity)
                .addApi(Auth.CREDENTIALS_API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, GoogleSignInOptions.DEFAULT_SIGN_IN);
        return getInstance(activity, builder);
    }

    public static CredentialsApiHelper getInstance(FragmentActivity activity,
                                                   GoogleApiClient.Builder builder) {
        CredentialsApiHelper helper = new CredentialsApiHelper();
        builder.enableAutoManage(activity, GoogleApiHelper.getSafeAutoManageId(), helper);
        builder.addConnectionCallbacks(helper);
        helper.setClient(builder.build());
        return helper;
    }

    public void setClient(GoogleApiClient client) {
        mClient = client;
    }

    public Task<Status> signOut() {
        final TaskCompletionSource<Status> statusTask = new TaskCompletionSource<>();
        mGoogleApiConnectionTask.getTask().addOnSuccessListener(new OnSuccessListener<Bundle>() {
            @Override
            public void onSuccess(Bundle bundle) {
                Auth.GoogleSignInApi.signOut(mClient)
                        .setResultCallback(new TaskResultCaptor<>(statusTask));
            }
        });
        return statusTask.getTask();
    }

    public Task<Status> disableAutoSignIn() {
        final TaskCompletionSource<Status> statusTask = new TaskCompletionSource<>();
        mGoogleApiConnectionTask.getTask().addOnSuccessListener(new OnSuccessListener<Bundle>() {
            @Override
            public void onSuccess(Bundle bundle) {
                Auth.CredentialsApi.disableAutoSignIn(mClient)
                        .setResultCallback(new TaskResultCaptor<>(statusTask));
            }
        });
        return statusTask.getTask();
    }

    public Task<Status> delete(final Credential credential) {
        final TaskCompletionSource<Status> statusTask = new TaskCompletionSource<>();
        mGoogleApiConnectionTask.getTask().addOnSuccessListener(new OnSuccessListener<Bundle>() {
            @Override
            public void onSuccess(Bundle bundle) {
                Auth.CredentialsApi.delete(mClient, credential)
                        .setResultCallback(new TaskResultCaptor<>(statusTask));
            }
        });
        return statusTask.getTask();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mGoogleApiConnectionTask.setResult(bundle);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Just wait
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        mGoogleApiConnectionTask.setException(new ConnectException(result.toString()));
    }

    private static final class TaskResultCaptor<R extends Result> implements ResultCallback<R> {
        private TaskCompletionSource<R> mSource;

        public TaskResultCaptor(TaskCompletionSource<R> source) {
            mSource = source;
        }

        @Override
        public void onResult(@NonNull R result) {
            mSource.setResult(result);
        }
    }
}
