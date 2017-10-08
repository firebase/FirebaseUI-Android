package com.firebase.ui.auth.data.remote;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.firebase.ui.auth.util.data.remote.GoogleApiConnector;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.net.ConnectException;

public class GoogleSignInHelper extends GoogleApiConnector implements LifecycleObserver {
    private interface AuthRunnable<T extends Result> {
        void run(ResultCallback<T> callback);
    }

    private final MutableLiveData<Boolean> mConnectionListener = new MutableLiveData<>();
    private final LifecycleOwner mOwner;

    private GoogleSignInHelper(GoogleApiClient.Builder builder, LifecycleOwner owner) {
        super(builder);
        mOwner = owner;

        owner.getLifecycle().addObserver(this);
    }

    public static GoogleSignInHelper newInstance(FragmentActivity activity) {
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(activity)
                .addApi(Auth.CREDENTIALS_API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, GoogleSignInOptions.DEFAULT_SIGN_IN);
        return new GoogleSignInHelper(builder, activity);
    }

    public Task<Status> signOut() {
        return injectActionBetweenConnection(new AuthRunnable<Status>() {
            @Override
            public void run(ResultCallback<Status> callback) {
                Auth.GoogleSignInApi.signOut(mClient).setResultCallback(callback);
            }
        });
    }

    public Task<Status> disableAutoSignIn() {
        return injectActionBetweenConnection(new AuthRunnable<Status>() {
            @Override
            public void run(ResultCallback<Status> callback) {
                Auth.CredentialsApi.disableAutoSignIn(mClient).setResultCallback(callback);
            }
        });
    }

    public Task<Status> delete(final Credential credential) {
        return injectActionBetweenConnection(new AuthRunnable<Status>() {
            @Override
            public void run(ResultCallback<Status> callback) {
                Auth.CredentialsApi.delete(mClient, credential).setResultCallback(callback);
            }
        });
    }

    private <T extends Result> Task<T> injectActionBetweenConnection(final AuthRunnable<T> r) {
        final TaskCompletionSource<T> task = new TaskCompletionSource<>();

        mConnectionListener.observe(mOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean isConnected) {
                if (isConnected) {
                    r.run(new ResultCallback<T>() {
                        @Override
                        public void onResult(@NonNull T t) {
                            task.setResult(t);
                        }
                    });
                } else {
                    task.setException(new ConnectException());
                }

                mConnectionListener.removeObserver(this);
            }
        });

        return task.getTask();
    }

    @Override
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void connect() {
        super.connect();
    }

    @Override
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    protected void disconnect() {
        super.disconnect();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void cleanup() {
        mOwner.getLifecycle().removeObserver(this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mConnectionListener.setValue(true);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        mConnectionListener.setValue(false);
    }
}
