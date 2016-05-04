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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.choreographer.ControllerConstants;

public class RegisterEmailActivity extends EmailFlowBaseActivity implements View.OnClickListener {
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EditText mNameEditText;

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
        setContentView(R.layout.register_email_layout);

        String email = getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);
        mEmailEditText = (EditText) findViewById(R.id.email);
        mPasswordEditText = (EditText) findViewById(R.id.password);
        mNameEditText = (EditText) findViewById(R.id.name);

        if (email != null) {
            mEmailEditText.setText(email);
        }

        Button createButton = (Button) findViewById(R.id.button_create);
        createButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(super.isPendingFinishing.get()) {
            return;
        }
       if (view.getId() == R.id.button_create) {
           Intent data = new Intent();
           data.putExtra(ControllerConstants.EXTRA_EMAIL, mEmailEditText.getText().toString());
           data.putExtra(ControllerConstants.EXTRA_NAME, mNameEditText.getText().toString());
           data.putExtra(ControllerConstants.EXTRA_PASSWORD, mPasswordEditText.getText().toString());
           finish(RESULT_OK, data);
       }
    }
}
