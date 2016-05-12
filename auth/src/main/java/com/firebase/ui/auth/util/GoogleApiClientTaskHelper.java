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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link com.google.android.gms.tasks.Task Task} based wrapper for acquiring a connected
 * {@link com.google.android.gms.common.api.GoogleApiClient} instance, which manages a single
 * instance per activity.
 */
public class GoogleApiClientTaskHelper {

    private static final IdentityHashMap<Activity, GoogleApiClientTaskHelper> INSTANCES
            = new IdentityHashMap<>();

    @NonNull
    private final Activity mActivity;

    @NonNull
    private final AtomicReference<GoogleApiClient> mClientRef;

    @NonNull
    private final AtomicReference<TaskCompletionSource<GoogleApiClient>> mConnectTaskRef;

    private GoogleApiClientTaskHelper(@NonNull Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("activity must not be null");
        }
        mActivity = activity;
        mClientRef = new AtomicReference<>();
        mConnectTaskRef = new AtomicReference<>();

        // ensure that when the activity is stopped, we release the reference to the
        // GoogleApiClient completion task, so that it can be garbage collected
        activity.getApplication().registerActivityLifecycleCallbacks(new DiscardOnActivityStop());
    }

    public Task<GoogleApiClient> getConnectedGoogleApiClient() {
        final TaskCompletionSource<GoogleApiClient> source = new TaskCompletionSource<>();
        if (!mConnectTaskRef.compareAndSet(null, source)) {
            return mConnectTaskRef.get().getTask();
        }

        final AtomicReference<GoogleApiClient> gacReference = new AtomicReference<>();
        GoogleApiClient client = new GoogleApiClient.Builder(mActivity)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        source.setResult(gacReference.get());
                    }

                    @Override
                    public void onConnectionSuspended(int i) {}
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        source.setException(new IOException(
                                "Failed to connect GoogleApiClient: "
                                        + connectionResult.getErrorMessage()));
                    }
                })
                .addApi(Auth.CREDENTIALS_API)
                .build();

        gacReference.set(client);
        client.connect();

        return source.getTask();
    }

    /**
     * Retrieve the instance for the specified activity, reusing an instance if it exists,
     * otherwise creates a new one.
     */
    public static GoogleApiClientTaskHelper getInstance(Activity activity) {
        GoogleApiClientTaskHelper helper = null;
        synchronized (INSTANCES) {
            helper = INSTANCES.get(activity);
            if (helper == null) {
                helper = new GoogleApiClientTaskHelper(activity);
                INSTANCES.put(activity, helper);
            }
        }
        return helper;
    }

    private static void clearInstance(Activity activity) {
        synchronized (INSTANCES) {
            INSTANCES.remove(activity);
        }
    }

    private final class DiscardOnActivityStop extends AbstractActivityLifecycleCallbacks {
        @Override
        public void onActivityStopped(Activity activity) {
            if (mActivity == activity) {
                GoogleApiClient client = mClientRef.get();
                if (client != null) {
                    client.disconnect();
                }

                mClientRef.set(null);
                mConnectTaskRef.set(null);
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
            clearInstance(activity);
        }
    }
}
