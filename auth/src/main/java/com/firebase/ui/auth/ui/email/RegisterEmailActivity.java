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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.account_link.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.ui.email.field_validators.EmailFieldValidator;
import com.firebase.ui.auth.ui.email.field_validators.PasswordFieldValidator;
import com.firebase.ui.auth.ui.email.field_validators.RequiredFieldValidator;
import com.firebase.ui.auth.util.FirebaseAuthWrapperFactory;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.List;

/**
 * Activity displaying a form to create a new email/password account.
 */
public class RegisterEmailActivity extends AppCompatBase implements View.OnClickListener, View.OnFocusChangeListener {
    private static final String TAG = "RegisterEmailActivity";
    private static final String PREV_EMAIL = "previous_email";
    private static final int RC_HINT = 13;
    private static final int RC_WELCOME_BACK_IDP = 15;
    private static final int RC_SIGN_IN = 16;

    private String mPrevEmail;
    private EditText mEmailEditText;
    private EditText mNameEditText;
    private EditText mPasswordEditText;
    private EmailFieldValidator mEmailFieldValidator;
    private PasswordFieldValidator mPasswordFieldValidator;
    private RequiredFieldValidator mNameValidator;
    @Nullable
    private SaveSmartLock mSaveSmartLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_email_layout);

        mSaveSmartLock = mActivityHelper.getSaveSmartLockInstance();

        mPasswordFieldValidator = new PasswordFieldValidator(
                (TextInputLayout) findViewById(R.id.password_layout),
                getResources().getInteger(R.integer.min_password_length));
        mNameValidator = new RequiredFieldValidator((TextInputLayout) findViewById(R.id.name_layout));
        mEmailFieldValidator = new EmailFieldValidator((TextInputLayout) findViewById(R.id.email_layout));

        mEmailEditText = (EditText) findViewById(R.id.email);
        mNameEditText = (EditText) findViewById(R.id.name);
        mPasswordEditText = (EditText) findViewById(R.id.password);

        mEmailEditText.setOnFocusChangeListener(this);
        mNameEditText.setOnFocusChangeListener(this);
        mPasswordEditText.setOnFocusChangeListener(this);
        findViewById(R.id.button_create).setOnClickListener(this);

        setUpTermsOfService();

        // Activity rotated
        if (savedInstanceState != null) {
            mPrevEmail = savedInstanceState.getString(PREV_EMAIL);
            return;
        }

        String email = getIntent().getStringExtra(ExtraConstants.EXTRA_EMAIL);
        if (!TextUtils.isEmpty(email)) {
            mEmailEditText.setText(email);
            mNameEditText.requestFocus();
            return;
        }

        if (mActivityHelper.getFlowParams().smartLockEnabled) {
            showEmailAutoCompleteHint();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(PREV_EMAIL, mPrevEmail);
        super.onSaveInstanceState(outState);
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
                // Getting default color
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
                @ColorInt int color = typedValue.data;

                new CustomTabsIntent.Builder()
                        .setToolbarColor(color)
                        .build()
                        .launchUrl(
                                RegisterEmailActivity.this,
                                Uri.parse(mActivityHelper.getFlowParams().termsOfServiceUrl));
            }
        });
    }

    private void showEmailAutoCompleteHint() {
        PendingIntent hintIntent = FirebaseAuthWrapperFactory
                .getFirebaseAuthWrapper(mActivityHelper.getAppName()).getEmailHintIntent(this);
        if (hintIntent != null) {
            try {
                startIntentSenderForResult(hintIntent.getIntentSender(), RC_HINT, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Unable to start hint intent", e);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_HINT:
                if (data != null) {
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    if (credential != null) {
                        mEmailEditText.setText(credential.getId());
                        String name = credential.getName();
                        mNameEditText.setText(name);
                        if (TextUtils.isEmpty(name)) {
                            mNameEditText.requestFocus();
                        } else {
                            mPasswordEditText.requestFocus();
                        }
                    }
                }
                break;
            case RC_SIGN_IN:
            case RC_WELCOME_BACK_IDP:
                finish(resultCode, data);
                break;
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (hasFocus) return; // Only consider fields losing focus

        int id = view.getId();
        if (id == R.id.email) {
            String email = mEmailEditText.getText().toString();
            if (mEmailFieldValidator.validate(mEmailEditText.getText())) {
                if (!email.equals(mPrevEmail)) {
                    mActivityHelper.showLoadingDialog(R.string.progress_dialog_checking_accounts);
                    checkAccountExists(email);
                    mPrevEmail = email;
                }
            }
        } else if (id == R.id.name) {
            mNameValidator.validate(mNameEditText.getText());
        } else if (id == R.id.password) {
            mPasswordFieldValidator.validate(mPasswordEditText.getText());
        }
    }

    public void checkAccountExists(final String email) {
        if (!TextUtils.isEmpty(email)) {
            mActivityHelper.getFirebaseAuth()
                    .fetchProvidersForEmail(email)
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error fetching providers for email"))
                    .addOnCompleteListener(this, new OnCompleteListener<ProviderQueryResult>() {
                        @Override
                        public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                            mActivityHelper.dismissDialog();
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<ProviderQueryResult>() {
                        @Override
                        public void onSuccess(ProviderQueryResult result) {
                            List<String> providers = result.getProviders();
                            if (providers != null && !providers.isEmpty()) {
                                // There is an account tied to this email.
                                // If only the email provider is associated with the account,
                                // direct the user to the email sign in flow.
                                // Otherwise, direct the user to sign in with the IDP they previously selected.

                                String provider = providers.get(0);
                                if (provider.equalsIgnoreCase(EmailAuthProvider.PROVIDER_ID)) {
                                    Intent signInIntent = SignInActivity.createIntent(
                                            RegisterEmailActivity.this,
                                            mActivityHelper.getFlowParams(),
                                            email);
                                    mActivityHelper.startActivityForResult(signInIntent,
                                                                           RC_SIGN_IN);
                                } else {
                                    Intent intent = WelcomeBackIdpPrompt.createIntent(
                                            RegisterEmailActivity.this,
                                            mActivityHelper.getFlowParams(),
                                            provider,
                                            null,
                                            email);
                                    mActivityHelper.startActivityForResult(intent,
                                                                           RC_WELCOME_BACK_IDP);
                                }
                            }
                        }
                    });
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

    private void registerUser(final String email, final String name, final String password) {
        mActivityHelper.getFirebaseAuth()
                .createUserWithEmailAndPassword(email, password)
                .addOnFailureListener(new TaskFailureLogger(TAG, "Error creating user"))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // Set display name
                        UserProfileChangeRequest changeNameRequest =
                                new UserProfileChangeRequest.Builder().setDisplayName(name).build();

                        final FirebaseUser user = authResult.getUser();
                        user.updateProfile(changeNameRequest)
                                .addOnFailureListener(new TaskFailureLogger(
                                        TAG, "Error setting display name"))
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        // This executes even if the name change fails, since
                                        // the account creation succeeded and we want to save
                                        // the credential to SmartLock (if enabled).
                                        mActivityHelper.saveCredentialsOrFinish(
                                                mSaveSmartLock,
                                                user,
                                                password);
                                    }
                                });
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
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
                            checkAccountExists(mEmailEditText.getText().toString());
                        } else {
                            // General error message, this branch should not be invoked but
                            // covers future API changes
                            emailInput.setError(getString(R.string.email_account_creation_error));
                        }
                    }
                });
    }

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return createIntent(context, flowParams, null);
    }

    public static Intent createIntent(Context context, FlowParameters flowParams, String email) {
        return BaseHelper.createBaseIntent(context, RegisterEmailActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_EMAIL, email);
    }
}
