package com.firebase.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.ui.com.firebasei.ui.authimpl.FacebookAuthHelper;
import com.firebase.ui.com.firebasei.ui.authimpl.FirebaseOAuthToken;
import com.firebase.ui.com.firebasei.ui.authimpl.GoogleAuthHelper;
import com.firebase.ui.com.firebasei.ui.authimpl.SocialProvider;
import com.firebase.ui.com.firebasei.ui.authimpl.TokenAuthHandler;
import com.firebase.ui.com.firebasei.ui.authimpl.TwitterAuthHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public abstract class FirebaseLoginBaseActivity extends AppCompatActivity {

    private final String TAG = "FirebaseLoginBaseAct";

    private GoogleAuthHelper mGoogleAuthHelper;
    private FacebookAuthHelper mFacebookAuthHelper;
    private TwitterAuthHelper mTwitterAuthHelper;
    private FirebaseLoginDialog mDialog;

    public SocialProvider mChosenProvider;

    /* Abstract methods for Login Events */
    protected abstract void onFirebaseLogin(AuthData authData);

    protected abstract void onFirebaseLogout();

    protected abstract void onFirebaseLoginError(FirebaseError firebaseError);

    protected abstract void onFirebaseLoginCancel();

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFacebookAuthHelper = new FacebookAuthHelper(this, new TokenAuthHandler() {
            @Override
            public void onTokenReceived(FirebaseOAuthToken token) {
                authenticateRefWithFirebaseOAuthToken(token);
            }

            @Override
            public void onCancelled() {
                onFirebaseLoginCancel();
            }

            @Override
            public void onError(Exception ex) {
                // TODO: Raise GMS Dialog Box?
            }
        });

        mGoogleAuthHelper = new GoogleAuthHelper(this, new TokenAuthHandler() {
            @Override
            public void onTokenReceived(FirebaseOAuthToken token) {
                authenticateRefWithFirebaseOAuthToken(token);
            }

            @Override
            public void onCancelled() {
                onFirebaseLoginCancel();
            }

            @Override
            public void onError(Exception ex) {
                // TODO: Raise GMS Dialog Box?
            }
        });

        mTwitterAuthHelper = new TwitterAuthHelper(this, new TokenAuthHandler() {
            @Override
            public void onTokenReceived(FirebaseOAuthToken token) {
                authenticateRefWithFirebaseOAuthToken(token);
            }

            @Override
            public void onCancelled() {
                onFirebaseLoginCancel();
            }

            @Override
            public void onError(Exception ex) {
                // TODO: Raise GMS Dialog Box?
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("BASE", "ACTIVITY" + data.toString());
        mFacebookAuthHelper.mCallbackManager.onActivityResult(requestCode, resultCode, data);
        mTwitterAuthHelper.onActivityResult(requestCode, resultCode, data);
    }

    public void showFirebaseLoginPrompt() {
        mDialog = new FirebaseLoginDialog();
        mDialog
            .addAuthHelper(mGoogleAuthHelper)
            .addAuthHelper(mFacebookAuthHelper)
            .addAuthHelper(mTwitterAuthHelper)
            .show(getFragmentManager(), "");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // TODO: is there a way to delay this? Or make it on-demand (i.e. make them call `startMonitoringState`)?
        // TODO: should we remove the authStateListener on `onStop()`?
        getFirebaseRef().addAuthStateListener(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if (authData != null) {
                    mChosenProvider = SocialProvider.valueOf(authData.getProvider());
                    if (mDialog != null) mDialog.dismiss();
                    onFirebaseLogin(authData);
                } else {
                    onFirebaseLogout();
                }
            }
        });
    }

    private void authenticateRefWithFirebaseOAuthToken(FirebaseOAuthToken token) {
        if (token.mode == FirebaseOAuthToken.SIMPLE) {
            // Simple mode is used for Facebook and Google auth
            getFirebaseRef().authWithOAuthToken(token.provider, token.token, new Firebase.AuthResultHandler() {
                @Override
                public void onAuthenticated(AuthData authData) {
                    // Do nothing. Auth updates are handled in the AuthStateListener
                }

                @Override
                public void onAuthenticationError(FirebaseError firebaseError) {
                    onFirebaseLoginError(firebaseError);
                }
            });
        } else if (token.mode == FirebaseOAuthToken.COMPLEX) {
            // Complex mode is used for Twitter auth
            Log.d(TAG, "Complex mode" + token.provider);
            Map<String, String> options = new HashMap<>();
            options.put("oauth_token", token.token);
            options.put("oauth_token_secret", token.secret);
            options.put("user_id", token.uid);

            getFirebaseRef().authWithOAuthToken(token.provider, options, new Firebase.AuthResultHandler() {
                @Override
                public void onAuthenticated(AuthData authData) {
                    // Do nothing. Auth updates are handled in the AuthStateListener
                }

                @Override
                public void onAuthenticationError(FirebaseError firebaseError) {
                    onFirebaseLoginError(firebaseError);
                }
            });
        }
    }

    private String getFirebaseUrlFromConfig() {
        String firebaseUrl;
        try {
            InputStream inputStream = getAssets().open("firebase-config.json");
            int size  = inputStream.available();
            byte[] buffer = new byte[size];

            inputStream.read(buffer);
            inputStream.close();

            String json = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(json);
            firebaseUrl = obj.getString("firebaseUrl");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } catch (JSONException ex) {
            ex.printStackTrace();
            return null;
        }

        return firebaseUrl;
    }
}
