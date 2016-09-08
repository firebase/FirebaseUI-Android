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

package com.firebase.ui.auth.ui.account_link;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.provider.IDPResponse;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.AuthCredentialHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.PasswordToggler;
import com.firebase.ui.auth.ui.email.RecoverPasswordActivity;
import com.firebase.ui.auth.util.SmartlockUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Activity to link a pre-existing email/password account to a new IDP sign-in by confirming
 * the password before initiating a link.
 */
public class WelcomeBackPasswordPrompt extends AppCompatBase implements View.OnClickListener {

    private static final int RC_CREDENTIAL_SAVE = 3;
    private static final String TAG = "WelcomeBackPassword";
    private static final StyleSpan BOLD = new StyleSpan(Typeface.BOLD);

    private String mEmail;
    private TextInputLayout mPasswordLayout;
    private EditText mPasswordField;
    private IDPResponse mIdpResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_back_password_prompt_layout);

        mPasswordLayout = (TextInputLayout) findViewById(R.id.password_layout);
        mPasswordField = (EditText) findViewById(R.id.password);

        mIdpResponse = getIntent().getParcelableExtra(ExtraConstants.EXTRA_IDP_RESPONSE);
        mEmail = mIdpResponse.getEmail();

        // Create welcome back text with email bolded
        String bodyText = getResources().getString(R.string.welcome_back_password_prompt_body);
        bodyText = String.format(bodyText, mEmail);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(bodyText);
        int emailStart = bodyText.indexOf(mEmail);
        spannableStringBuilder.setSpan(BOLD, emailStart, emailStart + mEmail.length(), Spannable
                .SPAN_INCLUSIVE_INCLUSIVE);

        TextView bodyTextView = ((TextView) findViewById(R.id.welcome_back_password_body));
        bodyTextView.setText(spannableStringBuilder);

        // Click listeners
        findViewById(R.id.button_done).setOnClickListener(this);
        findViewById(R.id.toggle_visibility).setOnClickListener(
                new PasswordToggler(mPasswordField));
        findViewById(R.id.trouble_signing_in).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == R.id.button_done) {
            mActivityHelper.showLoadingDialog(R.string.progress_dialog_signing_in);
            next(mEmail, mPasswordField.getText().toString());
        } else if (id == R.id.trouble_signing_in) {
            mActivityHelper.dismissDialog();
            startActivity(RecoverPasswordActivity.createIntent(
                    getApplicationContext(),
                    mActivityHelper.getFlowParams(),
                    mEmail));
            finish(RESULT_OK, new Intent());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_CREDENTIAL_SAVE) {
            finish(RESULT_OK, new Intent());
        }
    }

    private void next(String email, final String password) {
        final FirebaseAuth firebaseAuth = mActivityHelper.getFirebaseAuth();

        // Sign in with known email and the password provided
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Error signing in with email and password"))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // Get the social AuthCredential from the IDPResponse object, link
                        // it to the email/password account.
                        AuthCredential authCredential =
                                AuthCredentialHelper.getAuthCredential(mIdpResponse);
                        authResult.getUser().linkWithCredential(authCredential);
                        firebaseAuth.signOut();

                        // Sign in with the credential
                        firebaseAuth.signInWithCredential(authCredential)
                                .addOnFailureListener(
                                        new TaskFailureLogger(TAG, "Error signing in with credential"))
                                .addOnSuccessListener(
                                        new OnSuccessListener<AuthResult>() {
                                            @Override
                                            public void onSuccess(AuthResult authResult) {
                                                mActivityHelper.dismissDialog();
                                                SmartlockUtil.saveCredentialOrFinish(
                                                        WelcomeBackPasswordPrompt.this,
                                                        RC_CREDENTIAL_SAVE,
                                                        mActivityHelper.getFlowParams(),
                                                        authResult.getUser(),
                                                        password,
                                                        null /* provider */);
                                            }
                                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String error = e.getLocalizedMessage();
                        mPasswordLayout.setError(error);
                    }
                });
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            IDPResponse response) {
        return ActivityHelper.createBaseIntent(context, WelcomeBackPasswordPrompt.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, response);
    }
}
