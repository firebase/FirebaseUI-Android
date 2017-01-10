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

package com.firebase.ui.auth.ui.accountlink;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ResultCodes;
import com.firebase.ui.auth.provider.AuthCredentialHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.RecoverPasswordActivity;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Activity to link a pre-existing email/password account to a new IDP sign-in by confirming
 * the password before initiating a link.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WelcomeBackPasswordPrompt extends AppCompatBase implements View.OnClickListener {
    private static final String TAG = "WelcomeBackPassword";

    private String mEmail;
    private TextInputLayout mPasswordLayout;
    private EditText mPasswordField;
    private IdpResponse mIdpResponse;
    @Nullable
    private SaveSmartLock mSaveSmartLock;

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            IdpResponse response) {
        return BaseHelper.createBaseIntent(context, WelcomeBackPasswordPrompt.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, response);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_back_password_prompt_layout);

        // Show keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        mSaveSmartLock = mActivityHelper.getSaveSmartLockInstance();
        mIdpResponse = IdpResponse.fromResultIntent(getIntent());
        mEmail = mIdpResponse.getEmail();

        mPasswordLayout = (TextInputLayout) findViewById(R.id.password_layout);
        mPasswordField = (EditText) findViewById(R.id.password);

        // Create welcome back text with email bolded
        String bodyText = getString(R.string.welcome_back_password_prompt_body, mEmail);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(bodyText);
        int emailStart = bodyText.indexOf(mEmail);
        spannableStringBuilder.setSpan(new StyleSpan(Typeface.BOLD),
                                       emailStart,
                                       emailStart + mEmail.length(),
                                       Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        TextView bodyTextView = ((TextView) findViewById(R.id.welcome_back_password_body));
        bodyTextView.setText(spannableStringBuilder);

        // Click listeners
        findViewById(R.id.button_done).setOnClickListener(this);
        findViewById(R.id.trouble_signing_in).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == R.id.button_done) {
            next(mEmail, mPasswordField.getText().toString());
        } else if (id == R.id.trouble_signing_in) {
            startActivity(RecoverPasswordActivity.createIntent(
                    this,
                    mActivityHelper.getFlowParams(),
                    mEmail));
            finish(ResultCodes.CANCELED, IdpResponse.getErrorCodeIntent(ErrorCodes.UNKNOWN_ERROR));
        }
    }

    private void next(final String email, final String password) {
        // Check for null or empty password
        if (TextUtils.isEmpty(password)) {
            mPasswordLayout.setError(getString(R.string.required_field));
            return;
        } else {
            mPasswordLayout.setError(null);
        }
        mActivityHelper.showLoadingDialog(R.string.progress_dialog_signing_in);

        final FirebaseAuth firebaseAuth = mActivityHelper.getFirebaseAuth();
        // Sign in with known email and the password provided
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Error signing in with email and password"))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        AuthCredential authCredential =
                                AuthCredentialHelper.getAuthCredential(mIdpResponse);

                        // If authCredential is null, the user only has an email account.
                        // Otherwise, the user has an email account that we need to link to an idp.
                        if (authCredential == null) {
                            mActivityHelper.saveCredentialsOrFinish(
                                    mSaveSmartLock,
                                    authResult.getUser(),
                                    password,
                                    new IdpResponse(EmailAuthProvider.PROVIDER_ID, email));
                        } else {
                            authResult.getUser()
                                    .linkWithCredential(authCredential)
                                    .addOnFailureListener(new TaskFailureLogger(
                                            TAG, "Error signing in with credential"))
                                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                        @Override
                                        public void onSuccess(AuthResult authResult) {
                                            mActivityHelper.saveCredentialsOrFinish(
                                                    mSaveSmartLock,
                                                    authResult.getUser(),
                                                    mIdpResponse);
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mActivityHelper.dismissDialog();
                        String error = e.getLocalizedMessage();
                        mPasswordLayout.setError(error);
                    }
                });
    }
}
