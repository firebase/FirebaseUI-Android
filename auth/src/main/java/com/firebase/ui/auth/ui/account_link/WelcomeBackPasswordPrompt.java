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
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeBackPasswordPrompt extends AppCompatBase implements View.OnClickListener {
    private static final int RC_CREDENTIAL_SAVE = 3;
    private static final String TAG = "WelcomeBackPassword";
    final StyleSpan bold = new StyleSpan(Typeface.BOLD);
    private String mEmail;
    private TextInputLayout mPasswordLayout;
    private EditText mPasswordField;
    private IDPResponse mIdpResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.sign_in_title);
        setContentView(R.layout.welcome_back_password_prompt_layout);
        mPasswordLayout = (TextInputLayout) findViewById(R.id.password_layout);
        mIdpResponse = getIntent().getParcelableExtra(ExtraConstants.EXTRA_IDP_RESPONSE);
        mEmail = mIdpResponse.getEmail();
        TextView bodyTextView = (TextView) findViewById(R.id.welcome_back_password_body);
        String bodyText = getResources().getString(R.string.welcome_back_password_prompt_body);
        bodyText = String.format(bodyText, mEmail);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(bodyText);
        int emailStart = bodyText.indexOf(mEmail);
        spannableStringBuilder.setSpan(bold, emailStart, emailStart + mEmail.length(), Spannable
                .SPAN_INCLUSIVE_INCLUSIVE);
        bodyTextView.setText(spannableStringBuilder);
        Button signIn = (Button) findViewById(R.id.button_done);
        signIn.setOnClickListener(this);
        mPasswordField = (EditText) findViewById(R.id.password);
        ImageView toggleImage = (ImageView) findViewById(R.id.toggle_visibility);
        toggleImage.setOnClickListener(new PasswordToggler(mPasswordField));
        TextView troubleSigningIn = (TextView) findViewById(R.id.trouble_signing_in);
        troubleSigningIn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
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
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Error signing in with email and password"))
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    AuthCredential authCredential =
                            AuthCredentialHelper.getAuthCredential(mIdpResponse);
                    task.getResult().getUser().linkWithCredential(authCredential);
                    firebaseAuth.signOut();

                    firebaseAuth.signInWithCredential(authCredential)
                            .addOnFailureListener(
                                    new TaskFailureLogger(TAG, "Error signing in with credential"))
                            .addOnSuccessListener(
                                new OnSuccessListener<AuthResult>() {
                                    @Override
                                    public void onSuccess(AuthResult authResult) {
                                        FirebaseUser firebaseUser = authResult.getUser();
                                        String photoUrl = null;
                                        Uri photoUri = firebaseUser.getPhotoUrl();
                                        if (photoUri != null) {
                                            photoUrl = photoUri.toString();
                                        }
                                        mActivityHelper.dismissDialog();
                                        startActivityForResult(
                                                SaveCredentialsActivity.createIntent(
                                                        mActivityHelper.getApplicationContext(),
                                                        mActivityHelper.getFlowParams(),
                                                        firebaseUser.getDisplayName(),
                                                        firebaseUser.getEmail(),
                                                        password,
                                                        null,
                                                        photoUrl
                                                ), RC_CREDENTIAL_SAVE);
                                    }
                            });
                } else {
                    String error = task.getException().getLocalizedMessage();
                    mPasswordLayout.setError(error);
                }
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
