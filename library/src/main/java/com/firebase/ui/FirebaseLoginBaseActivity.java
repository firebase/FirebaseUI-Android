package com.firebase.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.ui.auth.FacebookAuthHelper;
import com.firebase.ui.auth.GoogleAuthHelper;
import com.firebase.ui.auth.PasswordAuthHelper;
import com.firebase.ui.auth.SocialProvider;
import com.firebase.ui.auth.TokenAuthHandler;
import com.firebase.ui.auth.TwitterAuthHelper;

public abstract class FirebaseLoginBaseActivity extends AppCompatActivity {

    private final String TAG = "FirebaseLoginBaseAct";

    private GoogleAuthHelper mGoogleAuthHelper;
    private FacebookAuthHelper mFacebookAuthHelper;
    private TwitterAuthHelper mTwitterAuthHelper;
    private PasswordAuthHelper mPasswordAuthHelper;
    private Firebase.AuthStateListener mAuthStateListener;

    TokenAuthHandler mHandler;

    private FirebaseLoginDialog mDialog;

    public SocialProvider mChosenProvider;

    /* Abstract methods for Login Events */
    protected abstract void onFirebaseLoginSuccess(AuthData authData);

    protected abstract void onFirebaseLogout();

    protected abstract void onFirebaseLoginProviderError(FirebaseError firebaseError);

    protected abstract void onFirebaseLoginUserError(FirebaseError firebaseError);

    /**
     * Subclasses of this activity must implement this method and return a valid Firebase reference that
     * can be used to call authentication related methods on.
     *
     * @return a Firebase reference that can be used to call authentication related methods on
     */
    protected abstract Firebase getFirebaseRef();

    /* Login/Logout */

    public void loginWithProvider(SocialProvider provider) {
        // TODO: what should happen if you're already authenticated?
        switch (provider) {
            case google:
                mGoogleAuthHelper.login();
                break;
            case facebook:
                mFacebookAuthHelper.login();
                break;
            case twitter:
                mTwitterAuthHelper.login();
                break;
        }

        mChosenProvider = provider;
    }

    public void logout() {
        switch (mChosenProvider) {
            case google:
                mGoogleAuthHelper.logout();
                break;
            case facebook:
                mFacebookAuthHelper.logout();
                break;
            case twitter:
                mFacebookAuthHelper.logout();
                break;
        }
        getFirebaseRef().unauth();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO: If someone isn't extending this activity, they need to implement this by hand
        if (mDialog.isActive) {
            mDialog.onActivityResult(requestCode, resultCode, data);
        } else {
            mFacebookAuthHelper.mCallbackManager.onActivityResult(requestCode, resultCode, data);
            mTwitterAuthHelper.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void showFirebaseLoginPrompt() {
        mDialog.show(getFragmentManager(), "");
    }

    @Override
    protected void onStart() {
        super.onStart();

        mHandler = new TokenAuthHandler() {
            @Override
            public void onSuccess(AuthData data) {

            }

            @Override
            public void onUserError(FirebaseError err) {
                onFirebaseLoginUserError(err);
            }

            @Override
            public void onProviderError(FirebaseError err) {
                onFirebaseLoginProviderError(err);
            }
        };

        mAuthStateListener = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if (authData != null) {
                    mChosenProvider = SocialProvider.valueOf(authData.getProvider());
                    onFirebaseLoginSuccess(authData);
                    Log.d(TAG, "Auth data changed");
                } else {
                    onFirebaseLogout();
                }
            }
        };

        mFacebookAuthHelper = new FacebookAuthHelper(this, getFirebaseRef(), mHandler);
        mGoogleAuthHelper = new GoogleAuthHelper(this, getFirebaseRef(), mHandler);
        mTwitterAuthHelper = new TwitterAuthHelper(this, getFirebaseRef(), mHandler);
        mPasswordAuthHelper = new PasswordAuthHelper(this, getFirebaseRef(), mHandler);

        mDialog = new FirebaseLoginDialog();
        mDialog
            .setContext(this)
            .setRef(getFirebaseRef())
            .setHandler(mHandler);

        mDialog
            .setProviderEnabled(SocialProvider.facebook)
            .setProviderEnabled(SocialProvider.google)
            .setProviderEnabled(SocialProvider.twitter)
            .setProviderEnabled(SocialProvider.password);

        getFirebaseRef().addAuthStateListener(mAuthStateListener);
    }

    protected void onStop() {
        super.onStop();
        getFirebaseRef().removeAuthStateListener(mAuthStateListener);
    }
}
