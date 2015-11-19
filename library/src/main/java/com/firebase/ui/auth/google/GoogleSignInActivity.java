package com.firebase.ui.auth.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.firebase.ui.auth.core.FirebaseResponse;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class GoogleSignInActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    public void onConnectionSuspended(int x) {

    }

    public void onConnected(Bundle x) {
        signIn();
    }

    private static final String TAG = "GoogleSignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String googleClientId = "";

        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            googleClientId = bundle.getString("com.firebase.ui.GoogleClientId");
        } catch (PackageManager.NameNotFoundException e) {
        } catch (NullPointerException e) {}

        if (googleClientId == null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("code", FirebaseResponse.MISSING_PROVIDER_APP_KEY);
            resultIntent.putExtra("error", "Missing Twitter key/secret, are they set in your AndroidManifest.xml?");
            setResult(GoogleActions.PROVIDER_ERROR, resultIntent);
            finish();
            return;
        }

        if (googleClientId.compareTo("") == 0) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("code", FirebaseResponse.INVALID_PROVIDER_APP_KEY);
            resultIntent.putExtra("error", "Invalid Google key, are they set in your res/values/strings.xml?");
            setResult(GoogleActions.PROVIDER_ERROR, resultIntent);
            finish();
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(googleClientId)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addConnectionCallbacks(this)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

//        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
//        if (opr.isDone()) {
//            GoogleSignInResult result = opr.get();
//            handleSignInResult(result);
//        } else {
//            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
//                @Override
//                public void onResult(GoogleSignInResult googleSignInResult) {
//                    handleSignInResult(googleSignInResult);
//                }
//            });
//        }
        //signOut();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, data.getExtras().toString());
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            acct.getServerAuthCode();
            GoogleOAuthTask googleOauthTask = new GoogleOAuthTask();
            googleOauthTask.setContext(this);
            googleOauthTask.execute(acct.getEmail());
        } else {
            // Signed out, show unauthenticated UI
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {

                    }
                });
    }

    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {

                    }
                });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("code", FirebaseResponse.MISC_PROVIDER_ERROR);
        resultIntent.putExtra("error", "onConnectionFailed:" + connectionResult);
        setResult(GoogleActions.PROVIDER_ERROR, resultIntent);
        finish();
    }
}

class GoogleOAuthTask extends AsyncTask<String, Integer, String> {
    private Context context;
    protected String doInBackground(String... emails) {
        String token = "";

        try {
            token = GoogleAuthUtil.getToken(context, emails[0], "oauth2:profile email");
        } catch (UserRecoverableAuthException e) {

        } catch (GoogleAuthException e) {

        } catch (java.io.IOException e) {

        }
        if (!token.equals("")) return token;
        else return "";
    }

    public void setContext(Context context) {
        this.context = context;
    }

    protected void onPostExecute(String token) {
        Activity activity = ((Activity)context);
        Intent resultIntent = new Intent();
        resultIntent.putExtra("oauth_token", token);
        activity.setResult(GoogleActions.SUCCESS, resultIntent);
        activity.finish();
    }
}
