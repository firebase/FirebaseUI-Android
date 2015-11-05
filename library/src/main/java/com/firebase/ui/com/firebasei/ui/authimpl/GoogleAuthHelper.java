package com.firebase.ui.com.firebasei.ui.authimpl;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.firebase.client.Firebase;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

import java.io.IOException;

/**
 *
 */
public class GoogleAuthHelper extends FirebaseAuthHelper implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private final String LOG_TAG = "GoogleAuthHelper";

    public static final String PROVIDER_NAME = "google";

    /* User provided callback class */
    private TokenAuthHandler mHandler;
    private Firebase mRef;

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_GOOGLE_SIGN_IN = 0;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;

    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;

    /* A flag indicating that a PendingIntent is in progress and prevents us from starting further intents. */
    private boolean mGoogleIntentInProgress;

    /* Store the connection result from onConnectionFailed callbacks so that we can resolve them when the user clicks
     * sign-in. */
    private ConnectionResult mGoogleConnectionResult;

    private Context mContext;

    private GoogleTokenTask mGoogleTokenTask;

    public GoogleAuthHelper(Context context, Firebase ref, TokenAuthHandler handler) {
        // Builder API
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(new Scope(Scopes.PROFILE))
                .build();

        // Activity Context
        mContext = context;

        mRef = ref;

        // User defined callbacks
        mHandler = handler;


        // Create a new GoogleAuthTokenTask
        mGoogleTokenTask = new GoogleTokenTask(mContext, mGoogleApiClient, new GoogleTokenHandler() {
            @Override
            public void onTokenReceived(String token) {
                FirebaseOAuthToken foToken = new FirebaseOAuthToken(
                        PROVIDER_NAME,
                        token);
                //mHandler.onTokenReceived(foToken);
            }

            @Override
            public void onUserRecoverableAuthException(UserRecoverableAuthException ex) {
                //mHandler.onError(ex);
            }

            @Override
            public void onGoogleAuthException(GoogleAuthException ex) {
                //mHandler.onError(ex);
            }

            @Override
            public void onIOException(IOException ex) {
                //mHandler.onError(ex);
            }
        });
    }

    public void login() {
        if (!mGoogleApiClient.isConnecting()) {
            if (mGoogleConnectionResult != null) {
                resolveSignInError();
            } else if (mGoogleApiClient.isConnected()) {
                mGoogleTokenTask.execute();
            } else {
                // connect API now
                Log.d(LOG_TAG, "Trying to connect to Google API");
                mGoogleApiClient.connect();
            }
        }
    }

    public void logout() {
        mGoogleApiClient.disconnect();
    }
    public String getProviderName() { return PROVIDER_NAME; }
    public Firebase getFirebaseRef() { return mRef; }

    @Override
    public void onConnected(Bundle bundle) {
        // onConnected indicates that an account was selected on the device, that the selected
        // account has granted any requested permissions to our app and that we were able to
        // establish a service connection to Google Play services.
        mGoogleTokenTask.execute();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("", Integer.valueOf(i).toString());
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!mGoogleIntentInProgress) {
            /* Store the ConnectionResult so that we can use it later when the user logs in */
            mGoogleConnectionResult = result;
            Log.e(LOG_TAG, result.toString());
            resolveSignInError();
        }
    }

    /* A helper method to resolve the current ConnectionResult error. */
    private void resolveSignInError() {
        Log.v(LOG_TAG, Boolean.valueOf(mGoogleConnectionResult.hasResolution()).toString());
        if (mGoogleConnectionResult.hasResolution()) {
            try {
                mGoogleIntentInProgress = true;
                mGoogleConnectionResult.startResolutionForResult((Activity) mContext, RC_GOOGLE_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mGoogleIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        } else {
            // Create error code?
            Log.e(LOG_TAG, mGoogleConnectionResult.toString());
        }
    }
}
