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
import android.util.Log;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class FacebookProvider implements IDPProvider, FacebookCallback<LoginResult> {
    public static final String ACCESS_TOKEN = "facebook_access_token";

    protected static final String ERROR = "err";
    protected static final String ERROR_MSG = "err_msg";

    private static final String TAG = "FacebookProvider";
    private static final String APPLICATION_ID = "application_id";
    private CallbackManager mCallbackManager;
    private IDPCallback mCallbackObject;

    public FacebookProvider (Context appContext, IDPProviderParcel facebookParcel) {
        mCallbackManager = CallbackManager.Factory.create();
        String applicationId = facebookParcel.getProviderExtra().getString(APPLICATION_ID);
        FacebookSdk.sdkInitialize(appContext);
        FacebookSdk.setApplicationId(applicationId);
    }

    public static IDPProviderParcel createFacebookParcel(String applicationId) {
        Bundle extra = new Bundle();
        extra.putString(APPLICATION_ID, applicationId);
        return new IDPProviderParcel(FacebookAuthProvider.PROVIDER_ID, extra);
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
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager loginManager = LoginManager.getInstance();
        loginManager.registerCallback(mCallbackManager, this);

        String[] permissions = activity.getResources().getStringArray(R.array.facebook_permissions);

        loginManager.logInWithReadPermissions(
                activity, Arrays.asList(permissions));
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
    public void onSuccess(final LoginResult loginResult) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Login to facebook successful with Application Id: "
                    + loginResult.getAccessToken().getApplicationId()
                    + " with Token: "
                    + loginResult.getAccessToken().getToken());
        }
        GraphRequest request = GraphRequest.newMeRequest(
                loginResult.getAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            String email = object.getString("email");
                            mCallbackObject.onSuccess(createIDPResponse(loginResult, email));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            mCallbackObject.onFailure(new Bundle());
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private IDPResponse createIDPResponse(LoginResult loginResult, String email) {
        Bundle response = new Bundle();
        response.putString(ACCESS_TOKEN, loginResult.getAccessToken().getToken());
        return new IDPResponse(FacebookAuthProvider.PROVIDER_ID, email, response);
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
