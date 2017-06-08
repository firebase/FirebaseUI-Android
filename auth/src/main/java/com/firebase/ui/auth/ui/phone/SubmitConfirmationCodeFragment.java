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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.ui.TermsTextView;

/**
 * Display confirmation code to verify phone numbers input in {{@link VerifyPhoneNumberFragment}}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SubmitConfirmationCodeFragment extends FragmentBase {

    public static final String TAG = "SubmitConfirmationCodeFragment";

    private static final long RESEND_WAIT_MILLIS = 15000;
    private static final String EXTRA_MILLIS_UNTIL_FINISHED = "EXTRA_MILLIS_UNTIL_FINISHED";

    private TextView mEditPhoneTextView;
    private TextView mResendCodeTextView;
    private TextView mCountDownTextView;
    private SpacedEditText mConfirmationCodeEditText;
    private Button mSubmitConfirmationButton;
    private CustomCountDownTimer mCountdownTimer;
    private PhoneVerificationActivity mVerifier;
    private TermsTextView mAgreementText;
    private long mMillisUntilFinished;

    public static SubmitConfirmationCodeFragment newInstance(FlowParameters flowParameters,
                                                             String phoneNumber) {
        SubmitConfirmationCodeFragment fragment = new SubmitConfirmationCodeFragment();

        Bundle args = new Bundle();
        args.putParcelable(ExtraConstants.EXTRA_FLOW_PARAMS, flowParameters);
        args.putString(ExtraConstants.EXTRA_PHONE, phoneNumber);

        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.confirmation_code_layout, container, false);
        FragmentActivity parentActivity = getActivity();

        mEditPhoneTextView = (TextView) v.findViewById(R.id.edit_phone_number);
        mCountDownTextView = (TextView) v.findViewById(R.id.ticker);
        mResendCodeTextView = (TextView) v.findViewById(R.id.resend_code);
        mConfirmationCodeEditText = (SpacedEditText) v.findViewById(R.id.confirmation_code);
        mSubmitConfirmationButton = (Button) v.findViewById(R.id.submit_confirmation_code);
        mAgreementText = (TermsTextView) v.findViewById(R.id.create_account_tos);

        final String phoneNumber = getArguments().getString(ExtraConstants.EXTRA_PHONE);

        parentActivity.setTitle(getString(R.string.verify_your_phone_title));
        setupConfirmationCodeEditText();
        setupEditPhoneNumberTextView(phoneNumber);
        setupCountDown(RESEND_WAIT_MILLIS);
        setupSubmitConfirmationCodeButton();
        setupResendConfirmationCodeTextView(phoneNumber);
        setUpTermsOfService();
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        mConfirmationCodeEditText.requestFocus();
        InputMethodManager imgr = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imgr.showSoftInput(mConfirmationCodeEditText, 0);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            Long t = savedInstanceState.getLong(EXTRA_MILLIS_UNTIL_FINISHED);
            if (t != null) {
                mCountdownTimer.update(t);
            }
        }

        if (!(getActivity() instanceof PhoneVerificationActivity)) {
            throw new IllegalStateException("Activity must implement PhoneVerificationHandler");
        }
        mVerifier = (PhoneVerificationActivity) getActivity();
    }

    @Override
    public void onDestroy() {
        cancelTimer();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_MILLIS_UNTIL_FINISHED, mMillisUntilFinished);
    }

    private void setTimer(long millisUntilFinished) {
        mCountDownTextView.setText(String.format(getString(R.string.resend_code_in),
                timeRoundedToSeconds(millisUntilFinished)));
    }

    private void setupResendConfirmationCodeTextView(final String phoneNumber) {
        mResendCodeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVerifier.verifyPhoneNumber(phoneNumber, true);
                mResendCodeTextView.setVisibility(View.GONE);
                mCountDownTextView.setVisibility(View.VISIBLE);
                mCountDownTextView.setText(String.format(getString(R.string.resend_code_in),
                        RESEND_WAIT_MILLIS / 1000));
                mCountdownTimer.renew();
            }
        });
    }

    private void setupCountDown(long startTimeMillis) {
        //set the timer view
        setTimer(startTimeMillis / 1000);

        //create a countdown
        mCountdownTimer = createCountDownTimer(mCountDownTextView, mResendCodeTextView, this,
                startTimeMillis);

        //start the countdown
        startTimer();
    }

    private void setupSubmitConfirmationCodeButton() {
        mSubmitConfirmationButton.setEnabled(false);

        mSubmitConfirmationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String confirmationCode = mConfirmationCodeEditText.getUnspacedText()
                        .toString();
                mVerifier.submitConfirmationCode(confirmationCode);
            }
        });
    }

    private void setupEditPhoneNumberTextView(@Nullable String phoneNumber) {
        mEditPhoneTextView.setText(TextUtils.isEmpty(phoneNumber) ? "" : phoneNumber);
        mEditPhoneTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                }
            }
        });
    }

    private void setupConfirmationCodeEditText() {
        mConfirmationCodeEditText.setText("------");
        BucketedTextChangeListener listener = createBucketedTextChangeListener();
        mConfirmationCodeEditText.addTextChangedListener(listener);
    }

    @NonNull
    private View.OnFocusChangeListener createOnFocusChangeListener() {
        return new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mConfirmationCodeEditText.setSelection(0);
                }
            }
        };
    }

    private BucketedTextChangeListener createBucketedTextChangeListener() {
        return new BucketedTextChangeListener(this.mConfirmationCodeEditText, 6, "-",
                createBucketOnEditCallback(mSubmitConfirmationButton));
    }

    private void startTimer() {
        if (mCountdownTimer != null) {
            mCountdownTimer.start();
        }
    }

    private void cancelTimer() {
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
        }
    }

    private void setUpTermsOfService() {
        mAgreementText.showTerms(mHelper.getFlowParams(), R.string.continue_phone_login);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CustomCountDownTimer getmCountdownTimer() {
        return mCountdownTimer;
    }

    private int timeRoundedToSeconds(double millis) {
        return (int) Math.ceil(millis / 1000);
    }

    private CustomCountDownTimer createCountDownTimer(final TextView timerText, final TextView
            resendCode, final SubmitConfirmationCodeFragment fragment, final long startTimeMillis) {
        return new CustomCountDownTimer(startTimeMillis, 500) {
            SubmitConfirmationCodeFragment mSubmitConfirmationCodeFragment = fragment;

            public void onTick(long millisUntilFinished) {
                mMillisUntilFinished = millisUntilFinished;
                mSubmitConfirmationCodeFragment.setTimer(millisUntilFinished);
            }

            public void onFinish() {
                timerText.setText("");
                timerText.setVisibility(View.GONE);
                resendCode.setVisibility(View.VISIBLE);
            }
        };
    }

    private BucketedTextChangeListener.ContentChangeCallback createBucketOnEditCallback(final
                                                                                        Button button) {
        return new BucketedTextChangeListener.ContentChangeCallback() {
            @Override
            public void whileComplete() {
                button.setEnabled(true);
            }

            @Override
            public void whileIncomplete() {
                button.setEnabled(false);
            }
        };
    }

    void setConfirmationCode(String code) {
        mConfirmationCodeEditText.setText(code);
    }
}
