package com.firebase.ui.auth.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.FirebaseResponse;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

class GoogleOAuthTask extends AsyncTask<String, Integer, String> {
    private final String TAG = "GoogleOAuthTask";

    private Context mContext;
    private GoogleOAuthTaskHandler mHandler;

    protected String doInBackground(String... emails) {
        String token = "";

        try {
            token = GoogleAuthUtil.getToken(mContext, emails[0], "oauth2:profile email");
            // since we're immediately exchanging this token for a Firebase JWT token, we don't need to store it
            GoogleAuthUtil.clearToken(mContext, token);
        } catch (UserRecoverableAuthException e) {
            Log.e(TAG, "Error getting token", e);
        } catch (GoogleAuthException e) {
            Log.e(TAG, "Error getting token", e);
        } catch (java.io.IOException e) {
            Log.e(TAG, "Error getting token", e);
        }
        if (!token.equals("")) return token;
        else return "";
    }

    public void setContext(Context context) {
        mContext = context;
    }
    public void setHandler(GoogleOAuthTaskHandler handler) { mHandler = handler; }

    protected void onPostExecute(String token) {
        if (token.equals("")) {
            mHandler.onOAuthFailure(new FirebaseLoginError(FirebaseResponse.MISC_PROVIDER_ERROR, "Fetching OAuth token from Google failed"));
        } else {
            mHandler.onOAuthSuccess(token);
        }
    }
}