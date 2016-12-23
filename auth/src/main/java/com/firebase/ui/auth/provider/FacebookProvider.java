/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StyleRes;
import android.util.Log;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FacebookProvider implements IdpProvider, FacebookCallback<LoginResult> {
    private static final String TAG = "FacebookProvider";
    private static final String EMAIL = "email";
    private static final String PUBLIC_PROFILE = "public_profile";
    private static final String ERROR = "err";
    private static final String ERROR_MSG = "err_msg";

    private static CallbackManager sCallbackManager;

    private final List<String> mScopes;
    // DO NOT USE DIRECTLY: see onSuccess(String, LoginResult) and onFailure(Bundle) below
    private IdpCallback mCallbackObject;

    public FacebookProvider(Context context, AuthUI.IdpConfig idpConfig, @StyleRes int theme) {
        Context appContext = context.getApplicationContext();

        if (appContext.getResources().getIdentifier(
                "facebook_permissions", "array", appContext.getPackageName()) != 0) {
            Log.w(TAG, "DEVELOPER WARNING: You have defined R.array.facebook_permissions but that"
                    + " is no longer respected as of FirebaseUI 1.0.0. Please see README for IDP"
                    + " scope configuration instructions.");
        }

        List<String> scopes = idpConfig.getScopes();
        if (scopes == null) {
            mScopes = new ArrayList<>();
        } else {
            mScopes = scopes;
        }
        FacebookSdk.sdkInitialize(appContext);
        FacebookSdk.setWebDialogTheme(theme);
    }

    public static AuthCredential createAuthCredential(IdpResponse response) {
        if (!response.getProviderType().equals(FacebookAuthProvider.PROVIDER_ID)) {
            return null;
        }
        return FacebookAuthProvider
                .getCredential(response.getIdpToken());
    }

    @Override
    public String getName(Context context) {
        return context.getResources().getString(R.string.idp_name_facebook);
    }

    @Override
    public String getProviderId() {
        return FacebookAuthProvider.PROVIDER_ID;
    }

    @Override
    public void startLogin(Activity activity) {
        sCallbackManager = CallbackManager.Factory.create();
        LoginManager loginManager = LoginManager.getInstance();
        loginManager.registerCallback(sCallbackManager, this);

        List<String> permissionsList = new ArrayList<>(mScopes);

        // Ensure we have email and public_profile scopes
        if (!permissionsList.contains(EMAIL)) {
            permissionsList.add(EMAIL);
        }

        if (!permissionsList.contains(PUBLIC_PROFILE)) {
            permissionsList.add(PUBLIC_PROFILE);
        }

        // Log in with permissions
        loginManager.logInWithReadPermissions(activity, permissionsList);
    }

    @Override
    public void setAuthenticationCallback(IdpCallback callback) {
        mCallbackObject = callback;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (sCallbackManager != null) {
            sCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSuccess(final LoginResult loginResult) {
        GraphRequest request = GraphRequest.newMeRequest(
                loginResult.getAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        FacebookRequestError requestError = response.getError();
                        if (requestError != null) {
                            Log.e(TAG,
                                  "Received Facebook error: " + requestError.getErrorMessage());
                            onFailure(new Bundle());
                            return;
                        }
                        if (object == null) {
                            Log.w(TAG, "Received null response from Facebook GraphRequest");
                            onFailure(new Bundle());
                        } else {
                            try {
                                String email = object.getString("email");
                                onSuccess(email, loginResult);
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON Exception reading from Facebook GraphRequest", e);
                                onFailure(new Bundle());
                            }
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    public void onCancel() {
        Bundle extra = new Bundle();
        extra.putString(ERROR, "cancelled");
        onFailure(extra);
    }

    @Override
    public void onError(FacebookException error) {
        Log.e(TAG, "Error logging in with Facebook. " + error.getMessage());
        Bundle extra = new Bundle();
        extra.putString(ERROR, "error");
        extra.putString(ERROR_MSG, error.getMessage());
        onFailure(extra);
    }

    private IdpResponse createIdpResponse(String email, LoginResult loginResult) {
        return new IdpResponse(
                FacebookAuthProvider.PROVIDER_ID,
                email,
                loginResult.getAccessToken().getToken());
    }

    private void onSuccess(String email, LoginResult loginResult) {
        gcCallbackManager();
        mCallbackObject.onSuccess(createIdpResponse(email, loginResult));
    }

    private void onFailure(Bundle bundle) {
        gcCallbackManager();
        mCallbackObject.onFailure(bundle);
    }

    private void gcCallbackManager() {
        // sCallbackManager must be static to prevent it from being destroyed if the activity
        // containing FacebookProvider dies.
        // In startLogin(Activity), LoginManager#registerCallback(CallbackManager, FacebookCallback)
        // stores the FacebookCallback parameter--in this case a FacebookProvider instance--into
        // a HashMap in the CallbackManager instance, sCallbackManager.
        // Because FacebookProvider which contains an instance of an activity, mCallbackObject,
        // is contained in sCallbackManager, that activity will not be garbage collected.
        // Thus, we have leaked an Activity.
        sCallbackManager = null;
    }
}
