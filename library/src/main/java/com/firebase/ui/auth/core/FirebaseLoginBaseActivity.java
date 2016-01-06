package com.firebase.ui.auth.core;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;

/**
 * You can subclass this activity in your app to easily get authentication working. If you already
 * have a base class and cannot switch, copy the relevant parts of this base activity into your own
 * activity.
 */
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
     * Subclasses of this activity should implement this method to handle any potential provider errors
     * like OAuth or other internal errors.
     *
     * @return void
     */
    protected abstract void onFirebaseLoginProviderError(FirebaseLoginError firebaseError);

    /**
     * Subclasses of this activity should implement this method to handle any potential user errors
     * like entering an incorrect password or closing the login dialog.
     *
     * @return void
     */
    protected abstract void onFirebaseLoginUserError(FirebaseLoginError firebaseError);

    /**
     * Calling this method will log out the currently authenticated user. It is only legal to call
     * this method after the `onStart()` method has completed.
     */
    public void logout() {
        mDialog.logout();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mDialog.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Calling this method displays the Firebase login dialog over the current activity, allowing the
     * user to authenticate with any of the configured providers. It is only legal to call this
     * method after the `onStart()` method has completed.
     */
    public void showFirebaseLoginPrompt() {
        mDialog.show(getFragmentManager(), "");
    }

    public void dismissFirebaseLoginPrompt() {
        mDialog.dismiss();
    }

    public void resetFirebaseLoginDialog() {
        mDialog.reset();
    }

    /**
     * Enables authentication with the specified provider.
     *
     * @param provider the provider to enable.
     */
    public void setEnabledAuthProvider(AuthProviderType provider) {
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

    @Override
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
