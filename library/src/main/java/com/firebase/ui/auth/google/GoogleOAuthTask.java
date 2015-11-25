package com.firebase.ui.auth.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.FirebaseResponse;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

class GoogleOAuthTask extends AsyncTask<String, Integer, String> {
    private Context mContext;
    private GoogleOAuthTaskHandler mHandler;

    protected String doInBackground(String... emails) {
        String token = "";

        try {
            token = GoogleAuthUtil.getToken(mContext, emails[0], "oauth2:profile email");
        } catch (UserRecoverableAuthException e) {

        } catch (GoogleAuthException e) {

        } catch (java.io.IOException e) {

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