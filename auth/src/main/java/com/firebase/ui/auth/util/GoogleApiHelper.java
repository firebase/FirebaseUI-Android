package com.firebase.ui.auth.util;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.net.ConnectException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Task} based wrapper to get a connect {@link GoogleApiClient}.
 */
public abstract class GoogleApiHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final AtomicInteger SAFE_ID = new AtomicInteger(10);

    protected GoogleApiClient mClient;
    private TaskCompletionSource<Bundle> mGoogleApiConnectionTask = new TaskCompletionSource<>();

    protected GoogleApiHelper(FragmentActivity activity, GoogleApiClient.Builder builder) {
        builder.enableAutoManage(activity, getSafeAutoManageId(), this);
        builder.addConnectionCallbacks(this);
        mClient = builder.build();
    }

    /**
     * @return a safe id for {@link GoogleApiClient.Builder#enableAutoManage(FragmentActivity, int,
     * GoogleApiClient.OnConnectionFailedListener)}
     */
    public static int getSafeAutoManageId() {
        return SAFE_ID.getAndIncrement();
    }

    public Task<Bundle> getConnectedApiTask() {
        return mGoogleApiConnectionTask.getTask();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // onConnected might be called multiple times, but we don't want to unregister listeners
        // because extenders might be relying on each onConnected call. Instead, we just ignore future
        // calls to onConnected or onConnectionFailed by using a `trySomething` strategy.
        mGoogleApiConnectionTask.trySetResult(bundle);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Just wait
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        mGoogleApiConnectionTask.trySetException(new ConnectException(result.toString()));
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

    protected static class ExceptionForwarder<TResult> implements OnCompleteListener<TResult> {
        private TaskCompletionSource mSource;
        private OnSuccessListener<TResult> mListener;

        public ExceptionForwarder(TaskCompletionSource source,
                                  OnSuccessListener<TResult> listener) {
            mSource = source;
            mListener = listener;
        }

        @Override
        public void onComplete(@NonNull Task<TResult> task) {
            if (task.isSuccessful()) {
                mListener.onSuccess(task.getResult());
            } else {
                mSource.setException(task.getException());
            }
        }
    }
}
