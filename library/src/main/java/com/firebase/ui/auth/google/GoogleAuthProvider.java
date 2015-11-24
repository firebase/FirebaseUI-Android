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
import com.firebase.ui.auth.core.FirebaseAuthProvider;
import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.FirebaseOAuthToken;
import com.firebase.ui.auth.core.FirebaseResponse;
import com.firebase.ui.auth.core.SocialProvider;
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

    public final static String PROVIDER_NAME = "google";
    public static final SocialProvider PROVIDER_TYPE = SocialProvider.google;
    private final String TAG = "GoogleAuthProvider";
    private GoogleApiClient mGoogleApiClient;
    private TokenAuthHandler mHandler;
    private Activity mActivity;
    private Firebase mRef;
    private Integer onConnectedAction;

    public GoogleAuthProvider(Context context, Firebase ref, TokenAuthHandler handler) {
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
    public SocialProvider getProviderType() { return PROVIDER_TYPE; };

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
    public void onConnectionSuspended(int i) {}

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
            mActivity.startActivityForResult(signInIntent, GoogleActions.SIGN_IN);
        } else {
            onConnectedAction = GoogleActions.SIGN_IN;
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
