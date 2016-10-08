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
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.field_validators.EmailFieldValidator;
import com.firebase.ui.auth.ui.email.field_validators.RequiredFieldValidator;
import com.firebase.ui.auth.util.SmartlockUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;

/**
 * Activity to sign in with email and password.
 */
public class SignInActivity extends AppCompatBase implements View.OnClickListener {
    private static final String TAG = "SignInActivity";
    private static final int RC_CREDENTIAL_SAVE = 101;

    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EmailFieldValidator mEmailValidator;
    private RequiredFieldValidator mPasswordValidator;
    private ImageView mTogglePasswordImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in_layout);

        String email = getIntent().getStringExtra(ExtraConstants.EXTRA_EMAIL);

        mEmailEditText = (EditText) findViewById(R.id.email);

        TypedValue visibleIcon = new TypedValue();
        TypedValue slightlyVisibleIcon = new TypedValue();

        getResources().getValue(R.dimen.visible_icon, visibleIcon, true);
        getResources().getValue(R.dimen.slightly_visible_icon, slightlyVisibleIcon, true);

        mPasswordEditText = (EditText) findViewById(R.id.password);
        mTogglePasswordImage = (ImageView) findViewById(R.id.toggle_visibility);

        mPasswordEditText.setOnFocusChangeListener(new ImageFocusTransparencyChanger(
                mTogglePasswordImage,
                visibleIcon.getFloat(),
                slightlyVisibleIcon.getFloat()));

        mTogglePasswordImage.setOnClickListener(new PasswordToggler(mPasswordEditText));

        mEmailValidator = new EmailFieldValidator((TextInputLayout) findViewById(R.id
                .email_layout));
        mPasswordValidator = new RequiredFieldValidator((TextInputLayout) findViewById(R.id
                .password_layout));
        Button signInButton = (Button) findViewById(R.id.button_done);
        TextView recoveryButton =  (TextView) findViewById(R.id.trouble_signing_in);

        if (email != null) {
            mEmailEditText.setText(email);
        }
        signInButton.setOnClickListener(this);
        recoveryButton.setOnClickListener(this);

    }

    @Override
    public void onBackPressed () {
        super.onBackPressed();
    }

    private void signIn(String email, final String password) {
        mActivityHelper.getFirebaseAuth()
                .signInWithEmailAndPassword(email, password)
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Error signing in with email and password"))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        mActivityHelper.dismissDialog();

                        // Save credential in SmartLock (if enabled)
                        SmartlockUtil.saveCredentialOrFinish(
                                SignInActivity.this,
                                RC_CREDENTIAL_SAVE,
                                mActivityHelper.getFlowParams(),
                                authResult.getUser(),
                                password,
                                null /* provider */);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mActivityHelper.dismissDialog();

                        // Show error message
                        TextInputLayout passwordInput =
                                (TextInputLayout) findViewById(R.id.password_layout);
                        passwordInput.setError(getString(R.string.login_error));
                        mActivityHelper.dismissDialog();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_CREDENTIAL_SAVE) {
            finish(RESULT_OK, new Intent());
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_done) {
            boolean emailValid = mEmailValidator.validate(mEmailEditText.getText());
            boolean passwordValid = mPasswordValidator.validate(mPasswordEditText.getText());
            if (!emailValid || !passwordValid) {
                return;
            } else {
                mActivityHelper.showLoadingDialog(R.string.progress_dialog_signing_in);
                signIn(mEmailEditText.getText().toString(), mPasswordEditText.getText().toString());
                return;
            }
        } else if (view.getId() == R.id.trouble_signing_in) {
            startActivity(RecoverPasswordActivity.createIntent(
                    this,
                    mActivityHelper.getFlowParams(),
                    mEmailEditText.getText().toString()));
            return;
        }
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            String email) {
        return ActivityHelper.createBaseIntent(context, SignInActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_EMAIL, email);
    }
}
