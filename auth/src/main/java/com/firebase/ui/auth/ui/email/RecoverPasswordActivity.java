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

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.util.ui.fieldvalidators.EmailFieldValidator;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

/**
 * Activity to initiate the "forgot password" flow by asking for the user's email.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RecoverPasswordActivity extends AppCompatBase implements View.OnClickListener, ImeHelper.DonePressedListener {
    private RecoverPasswordHandler mHandler;

    private TextInputLayout mEmailInputLayout;
    private EditText mEmailEditText;
    private EmailFieldValidator mEmailFieldValidator;

    public static Intent createIntent(Context context, FlowParameters params, String email) {
        return HelperActivityBase.createBaseIntent(context, RecoverPasswordActivity.class, params)
                .putExtra(ExtraConstants.EXTRA_EMAIL, email);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_forgot_password_layout);

        mHandler = ViewModelProviders.of(this).get(RecoverPasswordHandler.class);
        mHandler.init(getFlowHolder());
        mHandler.getPasswordResetListener().observe(this, new Observer<Task<String>>() {
            @Override
            public void onChanged(Task<String> task) {
                if (task.isSuccessful()) {
                    mEmailInputLayout.setError(null);
                    RecoveryEmailSentDialog.show(task.getResult(), getSupportFragmentManager());
                } else if (task.getException() instanceof FirebaseAuthInvalidUserException
                        || task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    // No FirebaseUser exists with this email address, show error.
                    mEmailInputLayout.setError(getString(R.string.fui_error_email_does_not_exist));
                } else {
                    mEmailInputLayout.setError(task.getException().getLocalizedMessage());
                }
            }
        });
        getFlowHolder().getProgressListener().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isDone) {
                if (isDone) {
                    getDialogHolder().dismissDialog();
                } else {
                    getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_sending);
                }
            }
        });

        mEmailInputLayout = findViewById(R.id.email_layout);
        mEmailEditText = findViewById(R.id.email);
        mEmailFieldValidator = new EmailFieldValidator(mEmailInputLayout);

        String email = getIntent().getStringExtra(ExtraConstants.EXTRA_EMAIL);
        if (email != null) {
            mEmailEditText.setText(email);
        }

        ImeHelper.setImeOnDoneListener(mEmailEditText, this);
        findViewById(R.id.button_done).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_done
                && mEmailFieldValidator.validate(mEmailEditText.getText())) {
            onDonePressed();
        }
    }

    @Override
    public void onDonePressed() {
        mHandler.startReset(mEmailEditText.getText().toString());
    }
}
