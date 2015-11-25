package com.firebase.ui.auth.facebook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.firebase.client.Firebase;
import com.firebase.ui.auth.core.FirebaseAuthProvider;
import com.firebase.ui.auth.core.FirebaseResponse;
import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.FirebaseOAuthToken;
import com.firebase.ui.auth.core.SocialProvider;
import com.firebase.ui.auth.core.TokenAuthHandler;

import java.util.Arrays;
import java.util.Collection;

public class FacebookAuthProvider extends FirebaseAuthProvider {

    public static final String PROVIDER_NAME = "facebook";
    public static final SocialProvider PROVIDER_TYPE = SocialProvider.facebook;
    private final String TAG = "FacebookAuthProvider";
    public CallbackManager mCallbackManager;
    private LoginManager mLoginManager;
    private TokenAuthHandler mHandler;
    private Activity mActivity;
    private Firebase mRef;
    private Boolean isReady = false;

    public FacebookAuthProvider(Context context, Firebase ref, final TokenAuthHandler handler) {
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
                    mHandler.onUserError(new FirebaseLoginError(FirebaseResponse.LOGIN_CANCELLED, "User closed login dialog."));
                }

                @Override
                public void onError(FacebookException ex) {
                    mHandler.onProviderError(new FirebaseLoginError(FirebaseResponse.MISC_PROVIDER_ERROR, ex.toString()));
                }
            }
        );

        String facebookAppId = "";

        try {
            ApplicationInfo ai = mActivity.getPackageManager().getApplicationInfo(mActivity.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            facebookAppId = bundle.getString("com.facebook.sdk.ApplicationId");
        } catch (PackageManager.NameNotFoundException e) {
        } catch (NullPointerException e) {}

        if (facebookAppId == null) {
            mHandler.onProviderError(new FirebaseLoginError(FirebaseResponse.MISSING_PROVIDER_APP_ID, "Missing Facebook Application ID, is it set in your AndroidManifest.xml?"));
            return;
        }

        if (facebookAppId.compareTo("") == 0) {
            mHandler.onProviderError(new FirebaseLoginError(FirebaseResponse.INVALID_PROVIDER_APP_ID, "Invalid Facebook Application ID, is it set in your res/values/strings.xml?"));
            return;
        }

        isReady = true;
    }

    public void login() {
        if (isReady) {
            Collection<String> permissions = Arrays.asList("public_profile");
            mLoginManager.logInWithReadPermissions(mActivity, permissions);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public String getProviderName() { return PROVIDER_NAME; }
    public SocialProvider getProviderType() { return PROVIDER_TYPE; };
    public Firebase getFirebaseRef() {return mRef; }

    public void logout() {
        mLoginManager.logOut();
    }
}
