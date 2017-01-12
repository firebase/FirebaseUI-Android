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
import android.support.annotation.NonNull;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

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

    public static CredentialsApiHelper getInstance(Activity activity) {
        // Get a task helper with the Credentials Api
        GoogleApiClientTaskHelper taskHelper = GoogleApiClientTaskHelper.getInstance(activity);
        taskHelper.getBuilder().addApi(Auth.CREDENTIALS_API);

        return getInstance(taskHelper);
    }

    public static CredentialsApiHelper getInstance(GoogleApiClientTaskHelper taskHelper) {
        return new CredentialsApiHelper(taskHelper);
    }

    public Task<Status> delete(final Credential credential) {
        return mClientHelper.getConnectedGoogleApiClient().continueWithTask(
                new ExceptionForwardingContinuation<GoogleApiClient, Status>() {
                    @Override
                    protected void process(
                            GoogleApiClient client,
                            TaskCompletionSource<Status> source) {
                        Auth.CredentialsApi.delete(client, credential)
                                .setResultCallback(new TaskResultCaptor<>(source));
                    }
                });
    }

    public Task<Status> disableAutoSignIn() {
        return mClientHelper.getConnectedGoogleApiClient().continueWithTask(
                new ExceptionForwardingContinuation<GoogleApiClient, Status>() {
                    @Override
                    protected void process(
                            final GoogleApiClient client,
                            final TaskCompletionSource<Status> source) {
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

    private abstract static class ExceptionForwardingContinuation<InT, OutT>
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

        protected abstract void process(InT in, TaskCompletionSource<OutT> result);
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
