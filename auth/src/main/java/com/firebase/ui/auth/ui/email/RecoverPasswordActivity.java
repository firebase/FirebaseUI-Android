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

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.util.ui.fieldvalidators.EmailFieldValidator;
import com.firebase.ui.auth.viewmodel.email.RecoverPasswordHandler;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

/**
 * Activity to initiate the "forgot password" flow by asking for the user's email.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RecoverPasswordActivity extends AppCompatBase implements View.OnClickListener,
        ImeHelper.DonePressedListener {
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
        mHandler.init(getFlowHolder().getArguments());
        mHandler.getProgressLiveData().observe(this, new Observer<Resource<String>>() {
            @Override
            public void onChanged(Resource<String> resource) {
                if (resource.getState() == State.LOADING) {
                    getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_sending);
                    return;
                }

                getDialogHolder().dismissDialog();
                if (resource.getState() == State.SUCCESS) {
                    mEmailInputLayout.setError(null);
                    showEmailSentDialog(resource.getValue());
                } else if (resource.getException() instanceof FirebaseAuthInvalidUserException
                        || resource.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    // No FirebaseUser exists with this email address, show error.
                    mEmailInputLayout.setError(getString(R.string.fui_error_email_does_not_exist));
                } else {
                    // Unknown error
                    mEmailInputLayout.setError(getString(R.string.fui_error_unknown));
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

    private void showEmailSentDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.fui_title_confirm_recover_password)
                .setMessage(getString(R.string.fui_confirm_recovery_body, email))
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish(Activity.RESULT_OK, new Intent());
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
