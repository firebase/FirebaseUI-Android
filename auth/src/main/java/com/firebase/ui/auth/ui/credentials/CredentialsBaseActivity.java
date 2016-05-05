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
import android.os.Bundle;
import android.os.Parcelable;

import com.firebase.ui.auth.api.CredentialsAPI;
import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.ui.BaseActivity;

import java.util.ArrayList;

public abstract class CredentialsBaseActivity extends BaseActivity {
    protected CredentialsAPI mCredentialsAPI;
    private ArrayList<Parcelable> mParcelables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParcelables = getIntent().getParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS);
    }

    @Override
    public void finish(int resultCode, Intent data) {
        data.putParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS, mParcelables);
        super.finish(resultCode, data);
    }

    @Override
    protected Controller setUpController() {
        mCredentialsAPI = new CredentialsAPI(this, new CredentialsAPI.CallbackInterface() {
            @Override
            public void onAsyncTaskFinished() {
                asyncTasksDone();
            }
        });
        return null;
    }

    /**
     * Override this method to handle async tasks. I.E.: if you need to wait until asyncTask(s)
     * will be done processing.
     */
    public void asyncTasksDone() {
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCredentialsAPI.isGoogleApiClient()){
            mCredentialsAPI.getGoogleApiClient().connect();
        }
    }

    @Override
    protected void onStop() {
        if (mCredentialsAPI.isGoogleApiClient() && mCredentialsAPI.getGoogleApiClient().isConnected()) {
            mCredentialsAPI.getGoogleApiClient().disconnect();
        }
        super.onStop();
    }

}
