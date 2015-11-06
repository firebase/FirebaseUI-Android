package com.firebase.ui.authimpl;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.Arrays;
import java.util.Collection;

public class FacebookAuthHelper extends FirebaseAuthHelper {

    private final String LOG_TAG = "FacebookAuthHelper";

    public static final String PROVIDER_NAME = "facebook";

    private LoginManager mLoginManager;
    public CallbackManager mCallbackManager;
    private TokenAuthHandler mHandler;
    private Activity mActivity;
    private Firebase mRef;

    public FacebookAuthHelper(Context context, Firebase ref, final TokenAuthHandler handler) {
        mActivity = (Activity) context;
        FacebookSdk.sdkInitialize(context.getApplicationContext());

        mLoginManager = LoginManager.getInstance();
        mCallbackManager = CallbackManager.Factory.create();
        mHandler = handler;
        mRef = ref;

        mLoginManager.registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        AccessToken token = loginResult.getAccessToken();

                        FirebaseOAuthToken foToken = new FirebaseOAuthToken(
                                PROVIDER_NAME,
                                token.getToken().toString());

                        onFirebaseTokenReceived(foToken, handler);
                    }

                    @Override
                    public void onCancel() {
                        mHandler.onUserError(new FirebaseError(0, "user_cancel"));
                    }

                    @Override
                    public void onError(FacebookException ex) {
                        mHandler.onProviderError(new FirebaseError(1, ex.toString()));
                    }
                }
        );
    }

    public void login() {
        Collection<String> permissions = Arrays.asList("public_profile");
        mLoginManager.logInWithReadPermissions(mActivity, permissions);
    }

    public String getProviderName() {
        return PROVIDER_NAME;
    }

    public Firebase getFirebaseRef() {return mRef; }

    public void logout() {
        mLoginManager.logOut();
    }
}
