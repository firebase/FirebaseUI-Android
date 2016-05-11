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
import com.firebase.ui.auth.api.FirebaseAuthWrapperFactory;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.firebase.ui.auth.ui.NoControllerBaseActivity;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.firebase.ui.auth.ui.email.field_validators.EmailFieldValidator;
import com.firebase.ui.auth.ui.email.field_validators.RequiredFieldValidator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class SignInActivity extends NoControllerBaseActivity implements View.OnClickListener {
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EmailFieldValidator mEmailValidator;
    private RequiredFieldValidator mPasswordValidator;
    private ImageView mTogglePasswordImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.sign_in);
        setContentView(R.layout.sign_in_layout);

        String email = getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);

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

    public static Intent createIntent(
            Context context,
            String appName,
            String email,
           ArrayList<IDPProviderParcel> providers
    ) {
        return new Intent(context, SignInActivity.class)
                .putExtra(ControllerConstants.EXTRA_PROVIDER, providers)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
                .putExtra(ControllerConstants.EXTRA_EMAIL, email);
    }

    @Override
    public void onBackPressed () {
        super.onBackPressed();
    }

    private void signIn(String email, final String password) {
        getFirebaseAuth()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        dismissDialog();
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = task.getResult().getUser();
                            if (FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(mAppName)
                                    .isPlayServicesAvailable(SignInActivity.this)) {
                                Intent saveCredentialIntent =
                                        SaveCredentialsActivity.createIntent(
                                                SignInActivity.this,
                                                firebaseUser.getDisplayName(),
                                                firebaseUser.getEmail(),
                                                password,
                                                null,
                                                null,
                                                mAppName
                                        );
                                startActivity(saveCredentialIntent);
                                finish(RESULT_OK, new Intent());
                            }
                        } else {
                            TextInputLayout passwordInput =
                                    (TextInputLayout) findViewById(R.id.password_layout);
                            passwordInput.setError(
                                    getString(com.firebase.ui.auth.R.string.login_error));
                        }
                    }
                });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_done) {
            boolean emailValid = mEmailValidator.validate(mEmailEditText.getText());
            boolean passwordValid = mPasswordValidator.validate(mPasswordEditText.getText());
            if (!emailValid || !passwordValid) {
                return;
            } else {
                showLoadingDialog(R.string.progress_dialog_signing_in);
                signIn(mEmailEditText.getText().toString(), mPasswordEditText.getText().toString());
                return;
            }
        } else if (view.getId() == R.id.trouble_signing_in) {
            startActivity(RecoverPasswordActivity.createIntent(
                    this, mAppName, mEmailEditText.getText().toString()));
            finish(RESULT_OK, new Intent());
            return;
        }
    }
}
