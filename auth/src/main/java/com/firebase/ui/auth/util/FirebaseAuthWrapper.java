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


import android.app.PendingIntent;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Abstraction layer for the Firebase Auth APIs ({@code com.google.firebase.auth}), that
 * provides synchronous convenience methods and facilitates mock testing.
 */
public interface FirebaseAuthWrapper {

    @WorkerThread
    boolean isExistingAccount(@Nullable String email);

    @WorkerThread
    @NonNull
    List<String> getProvidersForEmail(@Nullable String email);

    @WorkerThread
    boolean resetPasswordForEmail(@NonNull String email);

    @WorkerThread
    @Nullable
    FirebaseUser signInWithEmailPassword(@NonNull String email, @NonNull String password);

    @WorkerThread
    @Nullable
    FirebaseUser signInWithCredential(@NonNull AuthCredential credential);

    @WorkerThread
    @Nullable
    FirebaseUser getCurrentUser();

    @WorkerThread
    @Nullable
    FirebaseUser createUserWithEmailAndPassword(@NonNull String email, @NonNull String password)
            throws ExecutionException, InterruptedException;

    @WorkerThread
    FirebaseUser linkWithCredential(
            FirebaseUser user,
            AuthCredential credential)
            throws ExecutionException;

    boolean isPlayServicesAvailable(Context context);

    PendingIntent getEmailHintIntent(Context context);

    void setTimeOut(long timeoutMs);

}
