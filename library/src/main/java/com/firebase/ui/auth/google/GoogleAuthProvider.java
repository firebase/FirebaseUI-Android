package com.firebase.ui.auth.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.firebase.client.Firebase;
import com.firebase.ui.auth.core.FirebaseAuthProvider;
import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.FirebaseOAuthToken;
import com.firebase.ui.auth.core.FirebaseResponse;
import com.firebase.ui.auth.core.AuthProviderType;
import com.firebase.ui.auth.core.TokenAuthHandler;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class GoogleAuthProvider extends FirebaseAuthProvider implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleOAuthTaskHandler {

    private final String TAG = "GoogleAuthProvider";
    private GoogleApiClient mGoogleApiClient;
    private Integer onConnectedAction = 0;

    public GoogleAuthProvider(Context context, AuthProviderType providerType, String providerName, Firebase ref, TokenAuthHandler handler) {
        super(context, providerType, providerName, ref, handler);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .enableAutoManage((FragmentActivity) getContext(), this)
                .addConnectionCallbacks(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mGoogleApiClient.connect();

    }

    @Override
    public void onConnected(Bundle bundle) {
        if (onConnectedAction == GoogleActions.SIGN_IN) {
            login();
        } else if (onConnectedAction == GoogleActions.SIGN_OUT) {
            logout();
        }

        onConnectedAction = 0;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public void logout() {
        if (mGoogleApiClient.isConnected()) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            revokeAccess();
                        }
                    });
        } else {
            onConnectedAction = GoogleActions.SIGN_OUT;
        }
    }

    public void login() {
        if (mGoogleApiClient.isConnected()) {
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            ((Activity)getContext()).startActivityForResult(signInIntent, GoogleActions.SIGN_IN);
        } else {
            onConnectedAction = GoogleActions.SIGN_IN;
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }
    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {

                    }
                });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GoogleActions.SIGN_IN && resultCode == -1) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }

        if (requestCode == GoogleActions.SIGN_IN && resultCode == 0) {
            Log.d(TAG, data.getExtras().keySet().toString());
            getHandler().onLoginUserError(new FirebaseLoginError(FirebaseResponse.LOGIN_CANCELLED, "User closed login dialog."));
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            GoogleOAuthTask googleOAuthTask = new GoogleOAuthTask();
            googleOAuthTask.setContext(getContext());
            googleOAuthTask.setHandler(this);
            googleOAuthTask.execute(acct.getEmail());
        }
        else {
            getHandler().onProviderError(new FirebaseLoginError(FirebaseResponse.MISC_PROVIDER_ERROR, result.getStatus().toString()));
        }
    }


    public void onOAuthSuccess(String OAuthToken) {
        FirebaseOAuthToken token = new FirebaseOAuthToken(
                getProviderName(),
                OAuthToken);
        onFirebaseTokenReceived(token, getHandler());
    }

    public void onOAuthFailure(FirebaseLoginError firebaseError) {
        getHandler().onProviderError(firebaseError);
    }

    public void cleanUp() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
            mGoogleApiClient.stopAutoManage((FragmentActivity) getContext());
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        FirebaseLoginError error = new FirebaseLoginError(FirebaseResponse.MISC_PROVIDER_ERROR, connectionResult.toString());
        getHandler().onProviderError(error);}
}
