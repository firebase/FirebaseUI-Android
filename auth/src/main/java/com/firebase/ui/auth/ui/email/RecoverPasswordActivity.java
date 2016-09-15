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
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.field_validators.EmailFieldValidator;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

/**
 * Activity to initiate the "forgot password" flow by asking for the user's email.
 */
public class RecoverPasswordActivity extends AppCompatBase implements View.OnClickListener {
    private static final String TAG = "RecoverPasswordActivity";
    private static final int RC_CONFIRM = 3;

    private EditText mEmailEditText;
    private EmailFieldValidator mEmailFieldValidator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password_layout);
        String email = getIntent().getStringExtra(ExtraConstants.EXTRA_EMAIL);

        mEmailFieldValidator = new EmailFieldValidator(
                (TextInputLayout) findViewById(R.id.email_layout));

        mEmailEditText = (EditText) findViewById(R.id.email);
        if (email != null) {
            mEmailEditText.setText(email);
        }

        findViewById(R.id.button_done).setOnClickListener(this);
    }

    private void next(final String email) {
        FirebaseAuth firebaseAuth = mActivityHelper.getFirebaseAuth();
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Error sending password reset email"))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mActivityHelper.dismissDialog();

                        Intent confirmIntent = ConfirmRecoverPasswordActivity.createIntent(
                                RecoverPasswordActivity.this,
                                mActivityHelper.getFlowParams(),
                                email);
                        startActivityForResult(confirmIntent, RC_CONFIRM);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mActivityHelper.dismissDialog();

                        if (e instanceof FirebaseAuthInvalidUserException) {
                            // No FirebaseUser exists with this email address, show error.
                            mEmailEditText.setError(getString(R.string.error_email_does_not_exist));
                        }
                    }
                });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_CONFIRM) {
            finish(RESULT_OK, new Intent());
        }
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_done) {
            if (!mEmailFieldValidator.validate(mEmailEditText.getText())) {
                return;
            }
            mActivityHelper.showLoadingDialog(R.string.progress_dialog_sending);
            next(mEmailEditText.getText().toString());
        }
    }

    public static Intent createIntent(Context context, FlowParameters flowParams, String email) {
        return ActivityHelper.createBaseIntent(context, RecoverPasswordActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_EMAIL, email);
    }
}
