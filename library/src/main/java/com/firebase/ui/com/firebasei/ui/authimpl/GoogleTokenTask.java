package com.firebase.ui.com.firebasei.ui.authimpl;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import java.io.IOException;

public class GoogleTokenTask extends AsyncTask<Void, Void, String> {

    private final String LOG_TAG = "GoogleTokenTask";
    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private GoogleTokenHandler mHandler;
    private String errorMessage;
    private UserRecoverableAuthException mUserRecoverableAuthException;
    private GoogleAuthException mGoogleAuthException;
    private IOException mIOException;


    public GoogleTokenTask(Context context, GoogleApiClient apiClient, GoogleTokenHandler handler) {
        mContext = context;
        mGoogleApiClient = apiClient;
        mHandler = handler;
    }

    @Override
    protected String doInBackground(Void... params) {
        String token = null;

        try {
            String scope = String.format("oauth2:%s", Scopes.PLUS_LOGIN);
            token = GoogleAuthUtil.getToken(mContext, Plus.AccountApi.getAccountName(mGoogleApiClient), scope);
        } catch (IOException transientEx) {
                    /* Network or server error */
            Log.e(LOG_TAG, "Error authenticating with Google: " + transientEx);
            errorMessage = "Network error: " + transientEx.getMessage();
            mIOException = transientEx;
            mHandler.onIOException(transientEx);
        } catch (UserRecoverableAuthException e) {
            Log.w(LOG_TAG, "Recoverable Google OAuth error: " + e.toString());
                    /* We probably need to ask for permissions, so start the intent if there is none pending */
            mUserRecoverableAuthException = e;
            mHandler.onUserRecoverableAuthException(e);
        } catch (GoogleAuthException authEx) {
                    /* The call is not ever expected to succeed assuming you have already verified that
                     * Google Play services is installed. */
            Log.e(LOG_TAG, "Error authenticating with Google: " + authEx.getMessage(), authEx);
            errorMessage = "Error authenticating with Google: " + authEx.getMessage();
            mGoogleAuthException = authEx;
            mHandler.onGoogleAuthException(authEx);
        }

        return token;
    }

    @Override
    protected void onPostExecute(String token) {
        if (token != null) {
            mHandler.onTokenReceived(token);
        }
    }
}

