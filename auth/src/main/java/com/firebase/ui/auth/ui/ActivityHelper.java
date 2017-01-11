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

package com.firebase.ui.auth.ui;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.firebase.auth.FirebaseUser;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ActivityHelper extends BaseHelper {
    private HelperActivityBase mActivity;

    public ActivityHelper(HelperActivityBase activity, Intent intent) {
        super(activity, (FlowParameters) intent.getParcelableExtra(ExtraConstants.EXTRA_FLOW_PARAMS));
        mActivity = activity;
    }

    public void configureTheme() {
        mActivity.setTheme(R.style.FirebaseUI); // Provides default values
        mActivity.setTheme(getFlowParams().themeId);
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        mActivity.startActivityForResult(intent, requestCode);
    }

    public void finish(int resultCode, Intent intent) {
        finishActivity(mActivity, resultCode, intent);
    }

    public SaveSmartLock getSaveSmartLockInstance() {
        return getSaveSmartLockInstance(mActivity);
    }

    public void saveCredentialsOrFinish(
            @Nullable SaveSmartLock saveSmartLock,
            FirebaseUser firebaseUser,
            @NonNull String password,
            IdpResponse response) {
        saveCredentialsOrFinish(saveSmartLock, mActivity, firebaseUser, password, response);
    }

    public void saveCredentialsOrFinish(
            @Nullable SaveSmartLock saveSmartLock,
            FirebaseUser firebaseUser,
            IdpResponse response) {
        saveCredentialsOrFinish(saveSmartLock, mActivity, firebaseUser, null, response);
    }
}
