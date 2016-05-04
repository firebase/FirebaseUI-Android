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

package com.firebase.ui.auth.ui.credentials;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.credentials.Credential;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.credentials.CredentialsController;

public class ChooseAccountActivity extends CredentialsBaseActivity{
    private static final String TAG = "ChooseAccountActivity";
    private static final int RC_CREDENTIALS_READ = 2;

    @Override
    protected Controller setUpController() {
        super.setUpController();
        return new CredentialsController(this, mCredentialsAPI, mAppName);
    }

    @Override
    public void asyncTasksDone() {
        mCredentialsAPI.resolveSavedEmails(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
        }

        if (requestCode == RC_CREDENTIALS_READ) {
            if (resultCode == RESULT_OK) {
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                mCredentialsAPI.handleCredential(credential);
                mCredentialsAPI.resolveSignIn();
                finish(RESULT_OK, getIntent());
            } else if (resultCode == RESULT_CANCELED) {
                finish(RESULT_OK, getIntent());
            } else if (resultCode == RESULT_FIRST_USER) {
                // TODO: (serikb) figure out flow
            }
        }
    }

}
