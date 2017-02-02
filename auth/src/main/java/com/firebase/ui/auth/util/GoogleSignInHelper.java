package com.firebase.ui.auth.util;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

public class GoogleSignInHelper extends GoogleApiHelper implements CredentialTaskApi {
    protected GoogleSignInHelper(FragmentActivity activity, GoogleApiClient.Builder builder) {
        super(activity, builder);
    }

    public static GoogleSignInHelper getInstance(FragmentActivity activity) {
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(activity)
                .addApi(Auth.CREDENTIALS_API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, GoogleSignInOptions.DEFAULT_SIGN_IN);
        return new GoogleSignInHelper(activity, builder);
    }

    public Task<Status> signOut() {
        final TaskCompletionSource<Status> statusTask = new TaskCompletionSource<>();
        getConnectedApiTask().addOnCompleteListener(new ExceptionForwarder<>(
                statusTask,
                new OnSuccessListener<Bundle>() {
                    @Override
                    public void onSuccess(Bundle status) {
                        Auth.GoogleSignInApi.signOut(mClient)
                                .setResultCallback(new TaskResultCaptor<>(statusTask));
                    }
                }));
        return statusTask.getTask();
    }

    @Override
    public Task<Status> disableAutoSignIn() {
        final TaskCompletionSource<Status> statusTask = new TaskCompletionSource<>();
        getConnectedApiTask().addOnCompleteListener(new ExceptionForwarder<>(
                statusTask,
                new OnSuccessListener<Bundle>() {
                    @Override
                    public void onSuccess(Bundle status) {
                        Auth.CredentialsApi.disableAutoSignIn(mClient)
                                .setResultCallback(new TaskResultCaptor<>(statusTask));
                    }
                }));
        return statusTask.getTask();
    }

    @Override
    public Task<Status> delete(final Credential credential) {
        final TaskCompletionSource<Status> statusTask = new TaskCompletionSource<>();
        getConnectedApiTask().addOnCompleteListener(new ExceptionForwarder<>(
                statusTask,
                new OnSuccessListener<Bundle>() {
                    @Override
                    public void onSuccess(Bundle status) {
                        Auth.CredentialsApi.delete(mClient, credential)
                                .setResultCallback(new TaskResultCaptor<>(statusTask));
                    }
                }));
        return statusTask.getTask();
    }
}
