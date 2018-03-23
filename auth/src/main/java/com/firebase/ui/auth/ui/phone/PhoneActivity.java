/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.firebase.ui.auth.ui.phone;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.PhoneNumberVerificationRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.FirebaseAuthError;
import com.firebase.ui.auth.util.ui.FlowUtils;
import com.firebase.ui.auth.viewmodel.idp.SocialProviderResponseHandler;
import com.google.firebase.auth.FirebaseAuthException;

/**
 * Activity to control the entire phone verification flow. Plays host to {@link
 * CheckPhoneNumberFragment} and {@link SubmitConfirmationCodeFragment}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PhoneActivity extends AppCompatBase {
    public static Intent createIntent(Context context, FlowParameters params, Bundle args) {
        return HelperActivityBase.createBaseIntent(context, PhoneActivity.class, params)
                .putExtra(ExtraConstants.EXTRA_PARAMS, args);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_activity_register_phone);

        final CheckPhoneNumberHandler handler =
                ViewModelProviders.of(this).get(CheckPhoneNumberHandler.class);
        handler.init(getFlowParams());
        handler.setProvidersHandler(
                ViewModelProviders.of(this).get(SocialProviderResponseHandler.class));
        handler.getVerificationErrorListener().observe(this, new Observer<Exception>() {
            @Override
            public void onChanged(@Nullable Exception e) {
                if (e instanceof PhoneNumberVerificationRequiredException) {
                    // Ignore if resending verification code
                    if (getSupportFragmentManager().findFragmentByTag(SubmitConfirmationCodeFragment.TAG) == null) {
                        showSubmitCodeFragment(
                                ((PhoneNumberVerificationRequiredException) e).getPhoneNumber());
                    }
                } else {
                    handleError(e);
                }
            }
        });
        handler.getOperation().observe(this, new Observer<Resource<IdpResponse>>() {
            @Override
            public void onChanged(Resource<IdpResponse> resource) {
                if (resource.getState() == State.LOADING) {
                    getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_loading);
                    return;
                }
                getDialogHolder().dismissDialog();

                if (resource.getState() == State.SUCCESS) {
                    startSaveCredentials(handler.getCurrentUser(), null, resource.getValue());
                } else {
                    Exception e = resource.getException();
                    if (!FlowUtils.handleError(PhoneActivity.this, e)) {
                        handleError(e);
                    }
                }
            }
        });

        if (savedInstanceState != null) { return; }

        Bundle params = getIntent().getExtras().getBundle(ExtraConstants.EXTRA_PARAMS);
        CheckPhoneNumberFragment fragment = CheckPhoneNumberFragment.newInstance(params);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_verify_phone, fragment, CheckPhoneNumberFragment.TAG)
                .disallowAddToBackStack()
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void handleError(Exception e) {
        TextInputLayout errorView = getErrorView();
        if (errorView == null) { return; }

        if (e instanceof FirebaseAuthException) {
            errorView.setError(getErrorMessage(
                    FirebaseAuthError.fromException((FirebaseAuthException) e)));
        } else {
            errorView.setError(e.getLocalizedMessage());
        }
    }

    private TextInputLayout getErrorView() {
        CheckPhoneNumberFragment checkFragment = (CheckPhoneNumberFragment)
                getSupportFragmentManager().findFragmentByTag(CheckPhoneNumberFragment.TAG);
        SubmitConfirmationCodeFragment submitFragment = (SubmitConfirmationCodeFragment)
                getSupportFragmentManager().findFragmentByTag(SubmitConfirmationCodeFragment.TAG);

        if (checkFragment != null && checkFragment.getView() != null) {
            return checkFragment.getView().findViewById(R.id.phone_layout);
        } else if (submitFragment != null && submitFragment.getView() != null) {
            return submitFragment.getView().findViewById(R.id.confirmation_code_layout);
        } else {
            return null;
        }
    }

    private String getErrorMessage(FirebaseAuthError error) {
        switch (error) {
            case ERROR_INVALID_PHONE_NUMBER:
                return getString(R.string.fui_invalid_phone_number);
            case ERROR_TOO_MANY_REQUESTS:
                return getString(R.string.fui_error_too_many_attempts);
            case ERROR_QUOTA_EXCEEDED:
                return getString(R.string.fui_error_quota_exceeded);
            case ERROR_INVALID_VERIFICATION_CODE:
                return getString(R.string.fui_incorrect_code_dialog_body);
            case ERROR_SESSION_EXPIRED:
                return getString(R.string.fui_error_session_expired);
            default:
                return error.getDescription();
        }
    }

    private void showSubmitCodeFragment(String number) {
        getSupportFragmentManager().beginTransaction()
                .replace(
                        R.id.fragment_verify_phone,
                        SubmitConfirmationCodeFragment.newInstance(number),
                        SubmitConfirmationCodeFragment.TAG)
                .addToBackStack(null)
                .commit();
    }
}
