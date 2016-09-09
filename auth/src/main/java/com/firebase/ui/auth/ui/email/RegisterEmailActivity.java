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
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
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
import com.firebase.ui.auth.ui.email.field_validators.PasswordFieldValidator;
import com.firebase.ui.auth.ui.email.field_validators.RequiredFieldValidator;
import com.firebase.ui.auth.util.SmartlockUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Activity displaying a form to create a new email/password account.
 */
public class RegisterEmailActivity extends AppCompatBase implements View.OnClickListener {

    private static final int RC_SAVE_CREDENTIAL = 3;
    private static final String TAG = "RegisterEmailActivity";

    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EditText mNameEditText;
    private EmailFieldValidator mEmailFieldValidator;
    private PasswordFieldValidator mPasswordFieldValidator;
    private RequiredFieldValidator mNameValidator;
    private ImageView mTogglePasswordImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_email_layout);

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
            mEmailEditText.setEnabled(false);
        }
        setUpTermsOfService();
        Button createButton = (Button) findViewById(R.id.button_create);
        createButton.setOnClickListener(this);
    }

    private void setUpTermsOfService() {
        if (mActivityHelper.getFlowParams().termsOfServiceUrl == null) {
            return;
        }
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ContextCompat.getColor
                (getApplicationContext(), R.color.linkColor));

        String preamble = getResources().getString(R.string.create_account_preamble);
        String link = getResources().getString(R.string.terms_of_service);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(preamble + link);
        int start = preamble.length();
        spannableStringBuilder.setSpan(foregroundColorSpan, start, start + link.length(), 0);
        TextView agreementText = (TextView) findViewById(R.id.create_account_text);
        agreementText.setText(spannableStringBuilder);
        agreementText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse
                        (mActivityHelper.getFlowParams().termsOfServiceUrl));
                startActivity(intent);
            }
        });
    }

    private void registerUser(String email, final String name, final String password) {
        final FirebaseAuth firebaseAuth = mActivityHelper.getFirebaseAuth();
        // create the user
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnFailureListener(new TaskFailureLogger(TAG, "Error creating user"))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        final FirebaseUser firebaseUser = authResult.getUser();
                        UserProfileChangeRequest changeNameRequest =
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name).build();

                        // Set display name
                        firebaseUser.updateProfile(changeNameRequest)
                                .addOnFailureListener(new TaskFailureLogger(
                                        TAG, "Error setting display name"))
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        mActivityHelper.dismissDialog();

                                        // This executes even if the name change fails, since
                                        // the account creation succeeded and we want to save
                                        // the credential to SmartLock (if enabled).
                                        SmartlockUtil.saveCredentialOrFinish(
                                                RegisterEmailActivity.this,
                                                RC_SAVE_CREDENTIAL,
                                                mActivityHelper.getFlowParams(),
                                                firebaseUser,
                                                password,
                                                null /* provider */);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mActivityHelper.dismissDialog();

                        TextInputLayout emailInput =
                                (TextInputLayout) findViewById(R.id.email_layout);
                        TextInputLayout passwordInput =
                                (TextInputLayout) findViewById(R.id.password_layout);

                        if (e instanceof FirebaseAuthWeakPasswordException) {
                            // Password too weak
                            passwordInput.setError(getString(R.string.error_weak_password));
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            // Email address is malformed
                            emailInput.setError(getString(R.string.invalid_email_address));
                        } else if (e instanceof FirebaseAuthUserCollisionException) {
                            // Collision with existing user email
                            emailInput.setError(getString(R.string.error_user_collision));
                        } else {
                            // General error message, this branch should not be invoked but
                            // covers future API changes
                            emailInput.setError(getString(R.string.email_account_creation_error));
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SAVE_CREDENTIAL) {
            finish(RESULT_OK, new Intent());
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_create) {
            String email = mEmailEditText.getText().toString();
            String password = mPasswordEditText.getText().toString();
            String name = mNameEditText.getText().toString();

            boolean emailValid = mEmailFieldValidator.validate(email);
            boolean passwordValid = mPasswordFieldValidator.validate(password);
            boolean nameValid = mNameValidator.validate(name);
            if (emailValid && passwordValid && nameValid) {
                mActivityHelper.showLoadingDialog(R.string.progress_dialog_signing_up);
                registerUser(email, name, password);
            }
        }
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            String email) {
        return ActivityHelper.createBaseIntent(context, RegisterEmailActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_EMAIL, email);
    }
}
