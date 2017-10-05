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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.WebDialog;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.User;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.google.firebase.auth.FacebookAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FacebookProvider implements Provider, FacebookCallback<LoginResult> {
    private static final String TAG = "FacebookProvider";
    private static final String EMAIL = "email";
    private static final String PUBLIC_PROFILE = "public_profile";

    private static CallbackManager sCallbackManager;

    private final List<String> mScopes;

    public FacebookProvider(HelperActivityBase activity, AuthUI.IdpConfig idpConfig) {
        List<String> scopes = idpConfig.getScopes();
        if (scopes == null) {
            mScopes = new ArrayList<>();
        } else {
            mScopes = scopes;
        }
        WebDialog.setWebDialogTheme(theme);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_idp_button_facebook;
    }

    @Override
    public void startLogin(HelperActivityBase activity) {
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

        loginManager.logInWithReadPermissions(activity, permissionsList);
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
                            onFailure();
                            return;
                        }
                        if (object == null) {
                            Log.w(TAG, "Received null response from Facebook GraphRequest");
                            onFailure();
                        } else {
                            String email = null;
                            String name = null;
                            Uri photoUri = null;

                            try {
                                email = object.getString("email");
                            } catch (JSONException e) {
                                Log.e(TAG, "Failure retrieving Facebook email", e);
                            }
                            try {
                                name = object.getString("name");
                            } catch (JSONException ignored) {}
                            try {
                                photoUri = Uri.parse(object.getJSONObject("picture")
                                        .getJSONObject("data")
                                        .getString("url"));
                            } catch (JSONException ignored) {}

                            onSuccess(loginResult, email, name, photoUri);
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email,picture");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    public void onCancel() {
        onFailure();
    }

    @Override
    public void onError(FacebookException error) {
        Log.e(TAG, "Error logging in with Facebook. " + error.getMessage());
        onFailure();
    }

    private void onSuccess(LoginResult loginResult,
                           @Nullable String email,
                           String name,
                           Uri photoUri) {
        gcCallbackManager();
        mCallbackObject.onSuccess(new IdpResponse.Builder(
                new User.Builder(FacebookAuthProvider.PROVIDER_ID, email)
                        .setName(name)
                        .setPhotoUri(photoUri)
                        .build())
                .setToken(loginResult.getAccessToken().getToken())
                .build());
    }

    private void onFailure() {
        gcCallbackManager();
        mCallbackObject.onFailure();
    }
}
