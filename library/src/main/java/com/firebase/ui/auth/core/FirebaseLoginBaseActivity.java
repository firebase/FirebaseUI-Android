package com.firebase.ui.auth.core;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;

public abstract class FirebaseLoginBaseActivity extends AppCompatActivity {

    private final String TAG = "FirebaseLoginBaseAct";

    private Firebase.AuthStateListener mAuthStateListener;
    private FirebaseLoginDialog mDialog;
    private TokenAuthHandler mHandler;

    /**
     * Subclasses of this activity may implement this method to handle when a user logs in.
     *
     * @return void
     */
    protected abstract void onFirebaseLoggedIn(AuthData authData);

    /**
     * Subclasses of this activity may implement this method to handle when a user logs out.
     *
     * @return void
     */
    protected abstract void onFirebaseLoggedOut();

    /**
     * Subclasses of this activity may implement this method to handle any potential provider errors
     * like OAuth or other internal errors.
     *
     * @return void
     */
    protected abstract void onFirebaseLoginProviderError(FirebaseLoginError firebaseError);

    /**
     * Subclasses of this activity may implement this method to handle any potential user errors
     * like entering an incorrect password or closing the login dialog.
     *
     * @return void
     */
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
                    onFirebaseLoggedIn(authData);
                } else {
                    onFirebaseLoggedOut();
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
        mDialog.setEnabledProvider(provider);
    }

    protected void onStop() {
        super.onStop();
        getFirebaseRef().removeAuthStateListener(mAuthStateListener);
    }
}
