package com.firebase.ui.auth.util;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.net.ConnectException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Task} based wrapper to get a connect {@link GoogleApiClient}.
 */
public abstract class GoogleApiHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final AtomicInteger SAFE_ID = new AtomicInteger(0);

    protected GoogleApiClient mClient;
    private TaskCompletionSource<Bundle> mGoogleApiConnectionTask = new TaskCompletionSource<>();

    protected GoogleApiHelper(FragmentActivity activity, GoogleApiClient.Builder builder) {
        builder.enableAutoManage(activity, getSafeAutoManageId(), this);
        builder.addConnectionCallbacks(this);
        mClient = builder.build();
    }

    public static int getSafeAutoManageId() {
        return SAFE_ID.getAndIncrement();
    }

    public Task<Bundle> getConnectedApiTask() {
        return mGoogleApiConnectionTask.getTask();
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

    protected static final class TaskResultCaptor<R extends Result> implements ResultCallback<R> {
        private TaskCompletionSource<R> mSource;

        public TaskResultCaptor(TaskCompletionSource<R> source) {
            mSource = source;
        }

        @Override
        public void onResult(@NonNull R result) {
            mSource.setResult(result);
        }
    }

    protected abstract static class ExceptionForwarder<TResult> implements OnSuccessListener<TResult>, OnFailureListener {
        private TaskCompletionSource mSource;

        public ExceptionForwarder(TaskCompletionSource source) {
            mSource = source;
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            mSource.setException(e);
        }
    }
}
