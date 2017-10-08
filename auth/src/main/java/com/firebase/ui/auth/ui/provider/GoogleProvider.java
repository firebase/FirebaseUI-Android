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

package com.firebase.ui.auth.ui.provider;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.remote.GoogleSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.data.remote.GoogleApiConnector;
import com.firebase.ui.auth.util.ui.ActivityResult;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;

public class GoogleProvider implements Provider, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "GoogleProvider";
    private static final int RC_CONNECTION = 20;

    private final FlowHolder mFlowHolder;
    private final GoogleSignInHandler mHandler;
    private GoogleApiClient mClient;

    public GoogleProvider(HelperActivityBase activity, IdpConfig idpConfig) {
        this(activity, idpConfig, null);
    }

    public GoogleProvider(final HelperActivityBase activity,
                          IdpConfig idpConfig,
                          @Nullable String email) {
        mFlowHolder = activity.getFlowHolder();
        mHandler = ViewModelProviders.of(activity).get(GoogleSignInHandler.class);
        mHandler.init(new GoogleSignInHandler.Params(
                idpConfig, activity.getSignInHandler(), mFlowHolder));

        initClient(activity, email);

        mHandler.getSignInFailedNotifier().observe(activity, new Observer<Status>() {
            @Override
            public void onChanged(@Nullable Status status) {
                if (status.getStatusCode() == CommonStatusCodes.DEVELOPER_ERROR) {
                    Log.w(TAG, "Developer error: this application is misconfigured." +
                            " Check your SHA1 and package name in the Firebase console.");
                    Toast.makeText(activity, "Developer error.", Toast.LENGTH_SHORT).show();
                } else if (status.getStatusCode() == CommonStatusCodes.INVALID_ACCOUNT) {
                    mClient.stopAutoManage(activity);
                    mClient.disconnect();
                    initClient(activity, null);
                    startLogin(activity);
                }
            }
        });
        activity.getFlowHolder()
                .getOnActivityResult()
                .observe(activity, new Observer<ActivityResult>() {
                    @Override
                    public void onChanged(@Nullable ActivityResult result) {
                        if (result.getRequestCode() == RC_CONNECTION) {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                mClient.connect();
                            }
                        }
                    }
                });
    }

    private void initClient(HelperActivityBase activity, @Nullable String email) {
        mClient = new GoogleApiClient.Builder(activity)
                .enableAutoManage(activity, GoogleApiConnector.getSafeAutoManageId(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mHandler.getSignInOptions(email))
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
    public void startLogin(HelperActivityBase activity) {
        activity.startActivityForResult(
                Auth.GoogleSignInApi.getSignInIntent(mClient),
                GoogleSignInHandler.RC_SIGN_IN);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.w(TAG, "onConnectionFailed:" + result);
        if (result.hasResolution()) {
            mFlowHolder.getPendingIntentStarter()
                    .setValue(Pair.create(result.getResolution(), RC_CONNECTION));
        }
    }
}

