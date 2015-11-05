package com.firebase.ui.com.firebasei.ui.authimpl;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class FacebookAuthHelper implements FirebaseAuthHelper {

    private final String LOG_TAG = "FacebookAuthHelper";

    public final String PROVIDER_NAME = "facebook";

    private LoginManager mLoginManager;
    public CallbackManager mCallbackManager;
    private Context mContext;
    private TokenAuthHandler mHandler;
    private Activity mActivity;

    public FacebookAuthHelper(Context context, TokenAuthHandler handler) {
        mActivity = (Activity) context;
        FacebookSdk.sdkInitialize(context.getApplicationContext());

        mLoginManager = LoginManager.getInstance();
        mCallbackManager = CallbackManager.Factory.create();
        mContext = context;
        mHandler = handler;

        mLoginManager.registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        AccessToken token = loginResult.getAccessToken();

                        FirebaseOAuthToken foToken = new FirebaseOAuthToken(
                                PROVIDER_NAME,
                                token.getToken().toString());
                        mHandler.onTokenReceived(foToken);
                    }

                    @Override
                    public void onCancel() {
                        mHandler.onCancelled();
                    }

                    @Override
                    public void onError(FacebookException ex) {
                        mHandler.onError(ex);
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

    public void logout() {
        mLoginManager.logOut();
    }
}
