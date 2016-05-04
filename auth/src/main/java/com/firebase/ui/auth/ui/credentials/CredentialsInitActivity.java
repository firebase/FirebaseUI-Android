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

import android.content.Context;
import android.content.Intent;

import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.credentials.CredentialsController;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;

import java.util.ArrayList;

public class CredentialsInitActivity extends CredentialsBaseActivity {
    public static Intent createIntent(
            Context context, String appName, ArrayList<IDPProviderParcel> parcels) {
        return new Intent()
                .setClass(context, CredentialsInitActivity.class)
                .putExtra(EXTRA_ID, CredentialsController.ID_INIT)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
                .putParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS, parcels);
    }

    @Override
    protected Controller setUpController() {
        super.setUpController();
        return new CredentialsController(getApplicationContext(), mCredentialsAPI, mAppName);
    }

    @Override
    public void asyncTasksDone() {
        finish(RESULT_OK, getIntent());
    }
}
