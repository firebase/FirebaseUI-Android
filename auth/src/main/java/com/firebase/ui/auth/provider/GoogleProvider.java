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
import android.app.PendingIntent;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.GoogleApiHelper;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class GoogleProvider implements Provider, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "GoogleProvider";
    private static final int RC_CONNECTION = 20;

    private final GoogleSignInHandler mHolder;
    private final MutableLiveData<Pair<PendingIntent, Integer>> mPendingIntentStarter;
    private final GoogleApiClient mGoogleApiClient;

    public GoogleProvider(HelperActivityBase activity, IdpConfig idpConfig) {
        this(activity, idpConfig, null);
    }

    public GoogleProvider(HelperActivityBase activity,
                          IdpConfig idpConfig,
                          @Nullable String email) {
        mHolder = ViewModelProviders.of(activity).get(GoogleSignInHandler.class);
        mHolder.init(idpConfig);
        mPendingIntentStarter = activity.getPendingIntentStarter();

        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .enableAutoManage(activity, GoogleApiHelper.getSafeAutoManageId(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mHolder.getSignInOptions(email))
                .build();
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.fui_idp_name_google);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_idp_button_google;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_CONNECTION && resultCode == Activity.RESULT_OK) {
            mGoogleApiClient.connect();
        } else {
            mHolder.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void startLogin(Activity activity) {
        activity.startActivityForResult(
                Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient),
                GoogleSignInHandler.RC_SIGN_IN);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.w(TAG, "onConnectionFailed:" + result);
        if (result.hasResolution()) {
            mPendingIntentStarter.setValue(Pair.create(result.getResolution(), RC_CONNECTION));
        }
    }
}

