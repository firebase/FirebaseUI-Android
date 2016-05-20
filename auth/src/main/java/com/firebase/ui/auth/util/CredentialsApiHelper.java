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

import android.app.Activity;
import android.app.PendingIntent;
import android.support.annotation.NonNull;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.provider.IDPProviderParcel;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link com.google.android.gms.tasks.Task Task} based wrapper for the Smart Lock for Passwords
 * API.
 */
public class CredentialsApiHelper {

    @NonNull
    private final GoogleApiClientTaskHelper mClientHelper;

    private CredentialsApiHelper(GoogleApiClientTaskHelper gacHelper) {
        mClientHelper = gacHelper;
    }

    public CredentialRequest createCredentialRequest(List<IDPProviderParcel> providers) {
        boolean emailSupported = false;
        ArrayList<String> idps = new ArrayList<>();
        for (IDPProviderParcel provider : providers) {
            String providerId = provider.getProviderType();
            if (AuthUI.EMAIL_PROVIDER.equals(providerId)) {
                emailSupported = true;
            } else if (AuthUI.GOOGLE_PROVIDER.equals(providerId)) {
                idps.add(IdentityProviders.GOOGLE);
            } else if (AuthUI.FACEBOOK_PROVIDER.equals(providerId)) {
                idps.add(IdentityProviders.FACEBOOK);
            }
        }

        return new CredentialRequest.Builder()
                .setPasswordLoginSupported(emailSupported)
                .setAccountTypes(idps.toArray(new String[idps.size()]))
                .build();
    }

    public Task<CredentialRequestResult> request(final CredentialRequest request) {
        return mClientHelper.getConnectedGoogleApiClient().continueWithTask(
                new ExceptionForwardingContinuation<GoogleApiClient, CredentialRequestResult>() {
                    @Override
                    protected void process(
                            final GoogleApiClient client,
                            final TaskCompletionSource<CredentialRequestResult> source)
                            throws Exception {
                        Auth.CredentialsApi.request(client, request)
                                .setResultCallback(new TaskResultCaptor<>(source));
                    }
                });
    }

    public Task<Status> save(final Credential credential) {
        return mClientHelper.getConnectedGoogleApiClient().continueWithTask(
                new ExceptionForwardingContinuation<GoogleApiClient, Status>() {
                    @Override
                    protected void process(
                            GoogleApiClient client,
                            TaskCompletionSource<Status> source)
                            throws Exception {
                        Auth.CredentialsApi.save(client, credential)
                                .setResultCallback(new TaskResultCaptor<Status>(source));
                    }
                });
    }

    public Task<PendingIntent> getHintPickerIntent(final HintRequest request) {
        return mClientHelper.getConnectedGoogleApiClient().continueWithTask(
                new ExceptionForwardingContinuation<GoogleApiClient, PendingIntent>() {
                    @Override
                    protected void process(
                            GoogleApiClient client,
                            TaskCompletionSource<PendingIntent> source)
                            throws Exception {
                        source.setResult(Auth.CredentialsApi.getHintPickerIntent(client, request));
                    }
                });
    }

    public Task<Status> delete(final Credential credential) {
        return mClientHelper.getConnectedGoogleApiClient().continueWithTask(
                new ExceptionForwardingContinuation<GoogleApiClient, Status>() {
                    @Override
                    protected void process(
                            GoogleApiClient client,
                            TaskCompletionSource<Status> source) throws Exception {
                        Auth.CredentialsApi.delete(client, credential)
                                .setResultCallback(new TaskResultCaptor<Status>(source));
                    }
                });
    }

    public Task<Status> disableAutoSignIn() {
        return mClientHelper.getConnectedGoogleApiClient().continueWithTask(
                new ExceptionForwardingContinuation<GoogleApiClient, Status>() {
                    @Override
                    protected void process(
                            final GoogleApiClient client,
                            final TaskCompletionSource<Status> source)
                            throws Exception {
                        Auth.CredentialsApi.disableAutoSignIn(client)
                                .setResultCallback(new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(@NonNull Status status) {
                                        source.setResult(status);
                                    }
                                });
                    }
                });
    }

    public static CredentialsApiHelper getInstance(Activity activity) {
        return new CredentialsApiHelper(GoogleApiClientTaskHelper.getInstance(activity));
    }

    private static abstract class ExceptionForwardingContinuation<InT, OutT>
            implements Continuation<InT, Task<OutT>> {

        @Override
        public final Task<OutT> then(@NonNull Task<InT> task) throws Exception {
            TaskCompletionSource<OutT> source = new TaskCompletionSource<>();
            // calling task.getResult() will implicitly re-throw the exception of the original
            // task, which will be returned as the result for the output task. Similarly,
            // if process() throws an exception, this will be turned into the task result.
            process(task.getResult(), source);
            return source.getTask();
        }

        protected abstract void process(InT in, TaskCompletionSource<OutT> result) throws Exception;
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
