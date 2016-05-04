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

package com.firebase.ui.auth.ui.email;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;

import com.firebase.ui.auth.api.FactoryHeadlessAPI;
import com.firebase.ui.auth.api.HeadlessAPIWrapper;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.email.EmailFlowController;
import com.firebase.ui.auth.ui.BaseActivity;
import com.google.android.gms.auth.api.credentials.Credential;

public class EmailHintContainerActivity extends EmailFlowBaseActivity {
    private static final int RC_HINT = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mId = EmailFlowController.ID_SELECT_EMAIL;

        HeadlessAPIWrapper apiWrapper = FactoryHeadlessAPI.getHeadlessAPIWrapperInstance
                (mAppName);

        PendingIntent hintIntent = apiWrapper.getEmailHints(this);

        if(hintIntent != null) {
            try {
                startIntentSenderForResult(hintIntent.getIntentSender(), RC_HINT, null, 0, 0, 0);
                return;
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }
        finish(BaseActivity.RESULT_CANCELED, new Intent());
        return;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( requestCode == RC_HINT && data != null) {
            Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
            if(credential == null) {
                finish(BaseActivity.RESULT_CANCELED, new Intent());
                return;
            }
            Intent finishData = new Intent();
            finishData.putExtra(ControllerConstants.EXTRA_EMAIL, credential.getId());
            finish(BaseActivity.RESULT_OK, finishData);
            return;
        }
    }

    public static Intent getInitIntent(Context context, String appName) {
        return new Intent(context, EmailHintContainerActivity.class)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName);
    }
}
