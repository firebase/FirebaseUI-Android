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
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.ui.email.field_validators.EmailFieldValidator;

public class RecoverPasswordActivity extends EmailFlowBaseActivity implements View.OnClickListener {
    private EditText mEmailEditText;
    private EmailFieldValidator mEmailFieldValidator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.recover_password_title);
        setContentView(R.layout.forgot_password_layout);
        String email = getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);

        mEmailFieldValidator = new EmailFieldValidator((TextInputLayout) findViewById(R.id
                .email_layout));

        mEmailEditText = (EditText) findViewById(R.id.email);
        Button nextButton = (Button) findViewById(R.id.button_done);

        if (email != null) {
            mEmailEditText.setText(email);
        }
        nextButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (super.isPendingFinishing.get()) {
            return;
        }
        if (view.getId() == R.id.button_done) {
            if (!mEmailFieldValidator.validate(mEmailEditText.getText())) {
                return;
            }
            Intent data = new Intent();
            data.putExtra(ControllerConstants.EXTRA_EMAIL, mEmailEditText.getText().toString());
            finish(RESULT_OK, data);
        }
    }

    public static Intent createIntent(Context context, String appName, String email) {
        return new Intent().setClass(context, RecoverPasswordActivity.class)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
                .putExtra(ControllerConstants.EXTRA_EMAIL, email);
    }
}
