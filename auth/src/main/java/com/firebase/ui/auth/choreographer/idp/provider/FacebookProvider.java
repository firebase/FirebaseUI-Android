package com.firebase.ui.auth.choreographer.idp.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;

import java.util.Arrays;

public class FacebookProvider implements IDPProvider, FacebookCallback<LoginResult> {

    protected static final String ERROR = "err";
    protected static final String ERROR_MSG = "err_msg";

    private static final String TAG = "FacebookProvider";
    private static final String ACCESS_TOKEN = "facebook_access_token";
    private static final String APPLICATION_ID = "application_id";

    private CallbackManager mCallbackManager;
    private IDPCallback mCallbackObject;

    public FacebookProvider (Context appContext, IDPProviderParcel facebookParcel) {
        String applicationId = facebookParcel.getProviderExtra().getString(APPLICATION_ID);
        FacebookSdk.sdkInitialize(appContext);
        FacebookSdk.setApplicationId(applicationId);
    }

    public static IDPProviderParcel createFacebookParcel(String applicationId) {
        Bundle extra = new Bundle();
        extra.putString(APPLICATION_ID, applicationId);
        return new IDPProviderParcel(FacebookAuthProvider.PROVIDER_ID, extra);
    }

    public String getName(Context context) {
        return context.getResources().getString(R.string.idp_name_facebook);
    }

    @Override
    public View getLoginButton(Context context) {
        mCallbackManager = CallbackManager.Factory.create();
        LoginButton button = new LoginButton(context);
        //TODO: (zhaojiac) give option to pass in permissions
        button.setReadPermissions("public_profile", "email");
        button.registerCallback(mCallbackManager, this);
        return button;
    }

    @Override
    public void startLogin(Activity activity, String email) {
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager loginManager = LoginManager.getInstance();
        loginManager.registerCallback(mCallbackManager, this);
        loginManager.logInWithReadPermissions(
                activity, Arrays.asList("public_profile", "email"));
    }

    @Override
    public void setAuthenticationCallback(IDPCallback callback) {
        this.mCallbackObject = callback;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSuccess(LoginResult loginResult) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Login to facebook successful with Application Id: "
                    + loginResult.getAccessToken().getApplicationId()
                    + " with Token: "
                    + loginResult.getAccessToken().getToken());
        }
        mCallbackObject.onSuccess(createIDPResponse(loginResult));
    }

    private IDPResponse createIDPResponse(LoginResult loginResult) {
        Bundle response = new Bundle();
        response.putString(ACCESS_TOKEN, loginResult.getAccessToken().getToken());
        return new IDPResponse(FacebookAuthProvider.PROVIDER_ID, response);
    }

    public static AuthCredential createAuthCredential(IDPResponse response) {
        if (!response.getProviderType().equals(FacebookAuthProvider.PROVIDER_ID)) {
            return null;
        }
        return FacebookAuthProvider
                .getCredential(response.getResponse().getString(ACCESS_TOKEN));
    }

    @Override
    public void onCancel() {
        Bundle extra = new Bundle();
        extra.putString(ERROR, "cancelled");
        mCallbackObject.onFailure(extra);

    }

    @Override
    public void onError(FacebookException error) {
        Bundle extra = new Bundle();
        extra.putString(ERROR, "error");
        extra.putString(ERROR_MSG, error.getMessage());
        mCallbackObject.onFailure(extra);
    }
}
