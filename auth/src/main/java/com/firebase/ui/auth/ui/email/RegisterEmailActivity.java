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

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.ui.email.field_validators.EmailFieldValidator;
import com.firebase.ui.auth.ui.email.field_validators.PasswordFieldValidator;
import com.firebase.ui.auth.ui.email.field_validators.RequiredFieldValidator;

public class RegisterEmailActivity extends EmailFlowBaseActivity implements View.OnClickListener {
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EditText mNameEditText;
    private EmailFieldValidator mEmailFieldValidator;
    private PasswordFieldValidator mPasswordFieldValidator;
    private RequiredFieldValidator mNameValidator;

    @Override
    public void onBackPressed() {
        String email = mEmailEditText.getText().toString();
        Intent data = new Intent();
        data.putExtra(ControllerConstants.EXTRA_EMAIL, email);
        finish(BACK_IN_FLOW, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.create_an_account_title);
        setContentView(R.layout.register_email_layout);

        String email = getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);
        mEmailEditText = (EditText) findViewById(R.id.email);
        mPasswordEditText = (EditText) findViewById(R.id.password);
        ImageView toggleImage = (ImageView) findViewById(R.id.toggle_visibility);
        toggleImage.setOnClickListener(new PasswordToggler(mPasswordEditText));
        mNameEditText = (EditText) findViewById(R.id.name);

        mPasswordFieldValidator = new PasswordFieldValidator((TextInputLayout)
                findViewById(R.id.password_layout),
                getResources().getInteger(R.integer.min_password_length));
        mNameValidator = new RequiredFieldValidator((TextInputLayout)
                findViewById(R.id.name_layout));
        mEmailFieldValidator = new EmailFieldValidator((TextInputLayout) findViewById(R.id
                .email_layout));

        if (email != null) {
            mEmailEditText.setText(email);
        }
        setUpTermsOfService();
        Button createButton = (Button) findViewById(R.id.button_create);
        createButton.setOnClickListener(this);
    }

    private void setUpTermsOfService() {
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ContextCompat.getColor
                (getApplicationContext(), R.color.linkColor));

        String preamble = getResources().getString(R.string.create_account_preamble);
        String link = getResources().getString(R.string.terms_of_service);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(preamble + link);
        int start = preamble.length();
        spannableStringBuilder.setSpan(foregroundColorSpan, start, start + link.length(), 0);
        TextView agreementText = (TextView) findViewById(R.id.create_account_text);
        agreementText.setText(spannableStringBuilder);
    }

    @Override
    public void onClick(View view) {
        if(super.isPendingFinishing.get()) {
            return;
        }
        if (view.getId() == R.id.button_create) {
            String email = mEmailEditText.getText().toString();
            String password = mPasswordEditText.getText().toString();
            String name = mNameEditText.getText().toString();

            boolean emailValid = mEmailFieldValidator.validate(email);
            boolean passwordValid = mPasswordFieldValidator.validate(password);
            boolean nameValid = mNameValidator.validate(name);
            if (emailValid && passwordValid && nameValid) {
                Intent data = new Intent();
                data.putExtra(ControllerConstants.EXTRA_EMAIL, mEmailEditText.getText().toString());
                data.putExtra(ControllerConstants.EXTRA_NAME, mNameEditText.getText().toString());
                data.putExtra(ControllerConstants.EXTRA_PASSWORD, mPasswordEditText.getText().toString());
                finish(RESULT_OK, data);
            }
        }
    }
}
