package com.firebase.ui.auth.facebook;

import android.app.Activity;
import android.content.Context;
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
import com.firebase.ui.auth.core.FirebaseAuthHelper;
import com.firebase.ui.auth.core.FirebaseErrors;
import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.FirebaseOAuthToken;
import com.firebase.ui.auth.core.TokenAuthHandler;

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
    private Boolean isReady = false;

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
                    mHandler.onUserError(new FirebaseLoginError(FirebaseErrors.LOGIN_CANCELLED, "User closed login dialog"));
                }

                @Override
                public void onError(FacebookException ex) {
                    mHandler.onProviderError(new FirebaseLoginError(FirebaseErrors.MISC_PROVIDER_ERROR, ex.toString()));
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
            mHandler.onProviderError(new FirebaseLoginError(FirebaseErrors.MISSING_PROVIDER_APP_ID, "Missing Facebook Application ID, is it set in your AndroidManifest.xml?"));
            return;
        }

        if (facebookAppId.compareTo("") == 0) {
            mHandler.onProviderError(new FirebaseLoginError(FirebaseErrors.INVALID_PROVIDER_APP_ID, "Invalid Facebook Application ID, is it set in your res/values/strings.xml?"));
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

    public String getProviderName() {
        return PROVIDER_NAME;
    }

    public Firebase getFirebaseRef() {return mRef; }

    public void logout() {
        mLoginManager.logOut();
    }
}
