package com.firebase.ui.auth.core;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;

public abstract class FirebaseLoginBaseActivity extends AppCompatActivity {

    private final String TAG = "FirebaseLoginBaseAct";

    private Firebase.AuthStateListener mAuthStateListener;
    private AuthData mAuthData;
    private FirebaseLoginDialog mDialog;
    private TokenAuthHandler mHandler;


    /**
     * Subclasses of this activity must implement this method and return a valid Firebase reference that
     * can be used to call authentication related methods on. The method is guaranteed *not* to be called
     * before onCreate() has finished.
     *
     * @return a Firebase reference that can be used to call authentication related methods on
     */
    protected abstract Firebase getFirebaseRef();

    /**
     * Returns the data for the currently authenticated users, or null if no user is authenticated.
     *
     * @return the data for the currently authenticated users, or null if no user is authenticated.
     */
    public AuthData getAuth() {
        return mAuthData;
    }

    /**
     * Subclasses of this activity may implement this method to handle when a user logs in.
     *
     * @return void
     */
    protected void onFirebaseLoggedIn(AuthData authData) {
    }

    /**
     * Subclasses of this activity may implement this method to handle when a user logs out.
     *
     * @return void
     */
    protected void onFirebaseLoggedOut() {
    }

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

    public void setEnabledAuthProvider(SocialProvider provider) {
        mDialog.setEnabledProvider(provider);
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
                    mAuthData = authData;
                    onFirebaseLoggedIn(authData);
                } else {
                    mAuthData = null;
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

    protected void onStop() {
        super.onStop();
        getFirebaseRef().removeAuthStateListener(mAuthStateListener);
        mDialog.cleanUp();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDialog.cleanUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDialog.cleanUp();
    }
}
