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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.AcquireEmailHelper;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.email.field_validators.EmailFieldValidator;

public class SignInNoPasswordActivity extends AppCompatBase implements View.OnClickListener {
    private EditText mEmailEditText;
    private EmailFieldValidator mEmailFieldValidator;
    private AcquireEmailHelper mAcquireEmailHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAcquireEmailHelper = new AcquireEmailHelper(mActivityHelper);
        setTitle(R.string.sign_in_with_email);
        setContentView(R.layout.signin_no_password_layout);

        String email = getIntent().getStringExtra(ExtraConstants.EXTRA_EMAIL);
        mEmailFieldValidator = new EmailFieldValidator(
                (TextInputLayout) findViewById(R.id.input_layout_email));
        mEmailEditText = (EditText) findViewById(R.id.email);
        if (email != null) {
            mEmailEditText.setText(email);
        }

        // show the keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        Button button = (Button) findViewById(R.id.button_ok);
        button.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mAcquireEmailHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        if (!mEmailFieldValidator.validate(mEmailEditText.getText())) {
            return;
        }
        mActivityHelper.showLoadingDialog(R.string.progress_dialog_loading);
        String email = mEmailEditText.getText().toString();
        mAcquireEmailHelper.checkAccountExists(email);
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            String email) {
        return ActivityHelper.createBaseIntent(context, SignInNoPasswordActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_EMAIL, email);
    }
}
