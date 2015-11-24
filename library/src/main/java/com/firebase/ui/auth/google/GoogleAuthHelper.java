package com.firebase.ui.auth.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.firebase.client.Firebase;
import com.firebase.ui.auth.core.FirebaseAuthHelper;
import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.FirebaseOAuthToken;
import com.firebase.ui.auth.core.FirebaseResponse;
import com.firebase.ui.auth.core.TokenAuthHandler;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class GoogleAuthHelper extends FirebaseAuthHelper implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleOAuthTaskHandler {

    public final static String PROVIDER_NAME = "google";
    private static final int RC_SIGN_IN = 9001;
    private final String TAG = "GoogleAuthHelper";
    private GoogleApiClient mGoogleApiClient;
    private TokenAuthHandler mHandler;
    private Activity mActivity;
    private Firebase mRef;

    public GoogleAuthHelper(Context context, Firebase ref, TokenAuthHandler handler) {
        mActivity = (Activity) context;
        mRef = ref;
        mHandler = handler;

        String googleClientId = "";

        try {
            ApplicationInfo ai = mActivity.getPackageManager().getApplicationInfo(mActivity.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            googleClientId = bundle.getString("com.firebase.ui.GoogleClientId");
        } catch (PackageManager.NameNotFoundException e) {
        } catch (NullPointerException e) {}

        if (googleClientId == null) {
            FirebaseLoginError error = new FirebaseLoginError(
                    FirebaseResponse.MISSING_PROVIDER_APP_KEY,
                    "Missing Google client ID, is it set in your AndroidManifest.xml?");
            mHandler.onProviderError(error);
            return;
        }

        if (googleClientId.compareTo("") == 0) {
            FirebaseLoginError error = new FirebaseLoginError(
                    FirebaseResponse.INVALID_PROVIDER_APP_KEY,
                    "Invalid Google client ID, is it set in your res/values/strings.xml?");
            mHandler.onProviderError(error);
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(googleClientId)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                .enableAutoManage((FragmentActivity) mActivity, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    public String getProviderName() { return PROVIDER_NAME; }
    public Firebase getFirebaseRef() { return mRef; }

    public void logout() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        revokeAccess();
                    }
                });
    }

    public void login() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        mActivity.startActivityForResult(signInIntent, RC_SIGN_IN);
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
        if (requestCode == RC_SIGN_IN && resultCode == -1) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }

        if (requestCode == RC_SIGN_IN && resultCode == 0) {
            mHandler.onUserError(new FirebaseLoginError(FirebaseResponse.LOGIN_CANCELLED, "User closed login dialog."));
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            acct.getServerAuthCode();
            GoogleOAuthTask googleOAuthTask = new GoogleOAuthTask();
            googleOAuthTask.setContext(mActivity);
            googleOAuthTask.setHandler(this);
            googleOAuthTask.execute(acct.getEmail());
        }
    }


    public void onOAuthSuccess(String OAuthToken) {
        FirebaseOAuthToken token = new FirebaseOAuthToken(
                PROVIDER_NAME,
                OAuthToken);
        onFirebaseTokenReceived(token, mHandler);
    }

    public void onOAuthFailure(FirebaseLoginError firebaseError) {
        mHandler.onProviderError(firebaseError);
    }

    public void onStart() {
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    public void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
            mGoogleApiClient.stopAutoManage((FragmentActivity) mActivity);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        FirebaseLoginError error = new FirebaseLoginError(FirebaseResponse.MISC_PROVIDER_ERROR, connectionResult.toString());
        mHandler.onProviderError(error);}
}
