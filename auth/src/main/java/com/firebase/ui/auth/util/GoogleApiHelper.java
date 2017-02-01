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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Task} based wrapper for the GoogleApiClient.
 */
public class GoogleApiHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, CredentialApiHelper {
    private static final AtomicInteger SAFE_ID = new AtomicInteger(0);

    private GoogleApiClient mClient;
    private TaskCompletionSource<Bundle> mGoogleApiConnectionTask = new TaskCompletionSource<>();

    private GoogleApiHelper() {
    }

    public static int getSafeAutoManageId() {
        return SAFE_ID.getAndIncrement();
    }

    public static GoogleApiHelper getInstanceForSignInApi(FragmentActivity activity) {
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(activity)
                .addApi(Auth.CREDENTIALS_API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, GoogleSignInOptions.DEFAULT_SIGN_IN);
        return getInstance(activity, builder);
    }

    public static GoogleApiHelper getInstance(FragmentActivity activity,
                                              GoogleApiClient.Builder builder) {
        GoogleApiHelper helper = new GoogleApiHelper();
        builder.enableAutoManage(activity, getSafeAutoManageId(), helper);
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

    @Override
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

    @Override
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
