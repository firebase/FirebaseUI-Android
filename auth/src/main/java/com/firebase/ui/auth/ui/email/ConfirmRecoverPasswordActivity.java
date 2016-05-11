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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.firebase.ui.auth.ui.NoControllerBaseActivity;

import java.util.ArrayList;

public class ConfirmRecoverPasswordActivity
        extends NoControllerBaseActivity
        implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confirm_recovery_layout);
        setTitle(R.string.check_your_email);
        String email = getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);

        boolean isSuccess = getIntent().getBooleanExtra(ControllerConstants.EXTRA_SUCCESS, true);

        if (isSuccess) {
            String text = String.format(
                    getResources().getString(R.string.confirm_recovery_body),
                    email
            );
            ((TextView) findViewById(R.id.body_text)).setText(text);
        } else {
            ((TextView) findViewById(R.id.body_text)).setText(R.string.recovery_fail_body);
        }
        findViewById(R.id.button_done).setOnClickListener(this);
    }

    public static Intent createIntent(Context context, boolean success, String email, String
            appName, ArrayList<IDPProviderParcel> providers) {
        return new Intent(context, ConfirmRecoverPasswordActivity.class)
                .putExtra(ControllerConstants.EXTRA_SUCCESS, success)
                .putExtra(ControllerConstants.EXTRA_EMAIL, email)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
                .putParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS, providers);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_done) {
            finish(RESULT_OK, new Intent());
        }
    }
}
