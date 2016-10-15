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

package com.firebase.ui.auth.util.smartlock;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.util.FirebaseAuthWrapperFactory;
import com.firebase.ui.auth.util.PlayServicesHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseUser;

import static android.app.Activity.RESULT_OK;

public abstract class SmartLock extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        ResultCallback<Status>,
        GoogleApiClient.OnConnectionFailedListener {
    static final String TAG = "CredentialsSaveBase";
    static final int RC_SAVE = 100;
    static final int RC_UPDATE_SERVICE = 28;

    AppCompatBase mActivity;
    ActivityHelper mActivityHelper;
    String mName;
    String mEmail;
    String mPassword;
    String mProvider;
    String mProfilePictureUri;
    GoogleApiClient mCredentialsApiClient;

    @Override
    public void onConnectionSuspended(int i) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Connection suspended with code " + i);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "connection failed with " + connectionResult.getErrorMessage()
                    + " and code: " + connectionResult.getErrorCode());
        }
        Toast.makeText(mActivity, "An error has occurred.", Toast.LENGTH_SHORT).show();
    }

    void finishOk() {
        // For tests
        mActivity.setResult(RESULT_OK);

        mActivity.finish(RESULT_OK, mActivity.getIntent());
    }

    boolean initializeAndContinue(AppCompatBase activity,
                                  ActivityHelper helper,
                                  FirebaseUser firebaseUser,
                                  @Nullable String password,
                                  @Nullable String provider) {
        mActivity = activity;
        mActivityHelper = helper;
        mName = firebaseUser.getDisplayName();
        mEmail = firebaseUser.getEmail();
        mPassword = password;
        mProvider = provider;
        mProfilePictureUri = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl()
                .toString() : null;

        // If SmartLock is disabled or play services is not available, finish the Activity
        if (!helper.getFlowParams().smartLockEnabled
                || !PlayServicesHelper.getInstance(activity).isPlayServicesAvailable()
                || !FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(helper.getFlowParams().appName)
                .isPlayServicesAvailable(activity)) {
            finishOk();
            return false;
        } else {
            return true;
        }
    }
}
