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
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.viewmodel.email.WelcomeBackPasswordHandler;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;

/**
 * Activity to link a pre-existing email/password account to a new IDP sign-in by confirming the
 * password before initiating a link.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WelcomeBackPasswordPrompt extends AppCompatBase
        implements View.OnClickListener, ImeHelper.DonePressedListener {
    private static final String TAG = "WelcomeBackPassword";

    private User mUser;
    private WelcomeBackPasswordHandler mHandler;

    private TextInputLayout mPasswordLayout;
    private EditText mPasswordField;

    public static Intent createIntent(Context context, FlowParameters flowParams, User user) {
        return HelperActivityBase.createBaseIntent(context, WelcomeBackPasswordPrompt.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_USER, user);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_welcome_back_password_prompt_layout);

        // Show keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        mUser = User.getUser(getIntent());
        mPasswordLayout = findViewById(R.id.password_layout);
        mPasswordField = findViewById(R.id.password);

        ImeHelper.setImeOnDoneListener(mPasswordField, this);

        // Create welcome back text with email bolded.
        String bodyText =
                getString(R.string.fui_welcome_back_password_prompt_body, mUser.getEmail());

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(bodyText);
        int emailStart = bodyText.indexOf(mUser.getEmail());
        spannableStringBuilder.setSpan(new StyleSpan(Typeface.BOLD),
                emailStart,
                emailStart + mUser.getEmail().length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        TextView bodyTextView = findViewById(R.id.welcome_back_password_body);
        bodyTextView.setText(spannableStringBuilder);

        // Click listeners
        findViewById(R.id.button_done).setOnClickListener(this);
        findViewById(R.id.trouble_signing_in).setOnClickListener(this);

        // Initialize ViewModel with arguments
        mHandler = ViewModelProviders.of(this).get(WelcomeBackPasswordHandler.class);
        mHandler.init(getFlowParams());

        // Observe the state of the main auth operation
        mHandler.getOperation().observe(this, new Observer<Resource<IdpResponse>>() {
            @Override
            public void onChanged(@Nullable Resource<IdpResponse> resource) {
                onSignInOperation(resource);
            }
        });
    }

    private void onSignInOperation(@Nullable Resource<IdpResponse> resource) {
        if (resource == null) {
            Log.w(TAG, "Got null resource, ignoring.");
            return;
        }

        switch (resource.getState()) {
            case LOADING:
                getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_signing_in);
                break;
            case SUCCESS:
                getDialogHolder().dismissDialog();

                // This logic remains in the view since SmartLock is effectively a different
                // 'screen' after the sign-in process.
                startSaveCredentials(
                        mHandler.getCurrentUser(),
                        mHandler.getPendingPassword(),
                        resource.getValue());
                break;
            case FAILURE:
                getDialogHolder().dismissDialog();
                String message = getErrorMessage(resource.getException());
                mPasswordLayout.setError(message);
                break;
        }
    }

    private String getErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            // TODO: Add translated "wrong password" message
            return exception.getLocalizedMessage();
        }

        return exception.getLocalizedMessage();
    }

    private void onForgotPasswordClicked() {
        startActivity(RecoverPasswordActivity.createIntent(
                this,
                getFlowParams(),
                mUser.getEmail()));
    }

    @Override
    public void onDonePressed() {
        validateAndSignIn();
    }

    private void validateAndSignIn() {
        validateAndSignIn(mPasswordField.getText().toString());
    }

    private void validateAndSignIn(String password) {
        // Check for null or empty password
        if (TextUtils.isEmpty(password)) {
            mPasswordLayout.setError(getString(R.string.fui_required_field));
            return;
        } else {
            mPasswordLayout.setError(null);
        }

        AuthCredential authCredential = ProviderUtils.getAuthCredential(mIdpResponse);
        mHandler.startSignIn(mUser.getEmail(), password, mIdpResponse, authCredential);
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == R.id.button_done) {
            validateAndSignIn();
        } else if (id == R.id.trouble_signing_in) {
            onForgotPasswordClicked();
        }
    }
}
