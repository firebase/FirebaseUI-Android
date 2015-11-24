package com.firebase.ui.auth.core;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.ui.auth.facebook.FacebookAuthHelper;
import com.firebase.ui.auth.google.GoogleAuthHelper;
import com.firebase.ui.auth.password.PasswordAuthHelper;
import com.firebase.ui.auth.twitter.TwitterAuthHelper;

public abstract class FirebaseLoginBaseActivity extends AppCompatActivity {

    private final String TAG = "FirebaseLoginBaseAct";

    private Firebase.AuthStateListener mAuthStateListener;
    private FirebaseLoginDialog mDialog;
    private TokenAuthHandler mHandler;

    /* Abstract methods for Login Events */
    protected abstract void onFirebaseLoginSuccess(AuthData authData);

    protected abstract void onFirebaseLogout();

    protected abstract void onFirebaseLoginProviderError(FirebaseLoginError firebaseError);

    protected abstract void onFirebaseLoginUserError(FirebaseLoginError firebaseError);

    /**
     * Subclasses of this activity must implement this method and return a valid Firebase reference that
     * can be used to call authentication related methods on.
     *
     * @return a Firebase reference that can be used to call authentication related methods on
     */
    protected abstract Firebase getFirebaseRef();

    public void logout() {
        mDialog.logout();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mDialog.onActivityResult(requestCode, resultCode, data);
    }

    public void showFirebaseLoginPrompt() {
        mDialog.show(getFragmentManager(), "");
    }

    public void dismissFirebaseLoginPrompt() {
        mDialog.dismiss();
    }

    public void resetFirebaseLoginDialog() {
        mDialog.reset();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mHandler = new TokenAuthHandler() {
            @Override
            public void onSuccess(AuthData data) {
               /* onFirebaseLoginSuccess is called by the AuthStateListener below */
            }

            @Override
            public void onUserError(FirebaseLoginError err) {
                onFirebaseLoginUserError(err);
            }

            @Override
            public void onProviderError(FirebaseLoginError err) {
                onFirebaseLoginProviderError(err);
            }
        };

        mAuthStateListener = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if (authData != null) {
                    onFirebaseLoginSuccess(authData);
                } else {
                    onFirebaseLogout();
                }
            }
        };

        mDialog = new FirebaseLoginDialog();
        mDialog
            .setContext(this)
            .setRef(getFirebaseRef())
            .setHandler(mHandler);

        getFirebaseRef().addAuthStateListener(mAuthStateListener);
    }

    public void setEnabledAuthProvider(SocialProvider provider) {
        mDialog.setProviderEnabled(provider);
    }

    protected void onStop() {
        super.onStop();
        getFirebaseRef().removeAuthStateListener(mAuthStateListener);
    }
}
