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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.util.ui.BucketedTextChangeListener;
import com.firebase.ui.auth.util.ui.ImeHelper;

import java.util.concurrent.TimeUnit;

/**
 * Display confirmation code to verify phone numbers input in {{@link CheckPhoneNumberFragment}}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SubmitConfirmationCodeFragment extends FragmentBase {

    public static final String TAG = "SubmitConfirmationCodeFragment";

    private static final long RESEND_WAIT_MILLIS = 15000;
    private static final long TICK_INTERVAL_MILLIS = 500;
    private static final String EXTRA_MILLIS_UNTIL_FINISHED = "millis_until_finished";

    private final Handler mLooper = new Handler();
    private final Runnable mCountdown = new Runnable() {
        @Override
        public void run() {
            processCountdownTick();
        }
    };

    private PhoneNumberVerificationHandler mHandler;
    private String mPhoneNumber;

    private ProgressBar mProgressBar;
    private TextView mPhoneTextView;
    private TextView mResendCodeTextView;
    private TextView mCountDownTextView;
    private SpacedEditText mConfirmationCodeEditText;
    private Button mSubmitConfirmationButton;
    private long mMillisUntilFinished = RESEND_WAIT_MILLIS;

    public static SubmitConfirmationCodeFragment newInstance(String phoneNumber) {
        SubmitConfirmationCodeFragment fragment = new SubmitConfirmationCodeFragment();
        Bundle args = new Bundle();
        args.putString(ExtraConstants.PHONE, phoneNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = ViewModelProviders.of(requireActivity())
                .get(PhoneNumberVerificationHandler.class);
        mPhoneNumber = getArguments().getString(ExtraConstants.PHONE);
        if (savedInstanceState != null) {
            mMillisUntilFinished = savedInstanceState.getLong(EXTRA_MILLIS_UNTIL_FINISHED);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fui_confirmation_code_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mProgressBar = view.findViewById(R.id.top_progress_bar);
        mPhoneTextView = view.findViewById(R.id.edit_phone_number);
        mCountDownTextView = view.findViewById(R.id.ticker);
        mResendCodeTextView = view.findViewById(R.id.resend_code);
        mConfirmationCodeEditText = view.findViewById(R.id.confirmation_code);
        mSubmitConfirmationButton = view.findViewById(R.id.submit_confirmation_code);

        requireActivity().setTitle(getString(R.string.fui_verify_your_phone_title));
        processCountdownTick();
        setupSubmitConfirmationButton();
        setupConfirmationCodeEditText();
        setupEditPhoneNumberTextView();
        setupResendConfirmationCodeTextView();
        PrivacyDisclosureUtils.setupTermsOfServiceFooter(
                requireContext(),
                getFlowParams(),
                view.<TextView>findViewById(R.id.email_footer_tos_and_pp_text));
    }

    @Override
    public void onStart() {
        super.onStart();
        mConfirmationCodeEditText.requestFocus();
        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(mConfirmationCodeEditText, 0);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        mLooper.removeCallbacks(mCountdown);
        outState.putLong(EXTRA_MILLIS_UNTIL_FINISHED, mMillisUntilFinished);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove here in addition to onSaveInstanceState since it might not be called if finishing
        // for good.
        mLooper.removeCallbacks(mCountdown);
    }

    private void setupSubmitConfirmationButton() {
        mSubmitConfirmationButton.setEnabled(false);
        mSubmitConfirmationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCode();
            }
        });
    }

    private void setupConfirmationCodeEditText() {
        mConfirmationCodeEditText.setText("------");
        mConfirmationCodeEditText.addTextChangedListener(new BucketedTextChangeListener(
                mConfirmationCodeEditText, 6, "-",
                new BucketedTextChangeListener.ContentChangeCallback() {
                    @Override
                    public void whileComplete() {
                        mSubmitConfirmationButton.setEnabled(true);
                    }

                    @Override
                    public void whileIncomplete() {
                        mSubmitConfirmationButton.setEnabled(false);
                    }
                }));

        ImeHelper.setImeOnDoneListener(mConfirmationCodeEditText,
                new ImeHelper.DonePressedListener() {
                    @Override
                    public void onDonePressed() {
                        if (mSubmitConfirmationButton.isEnabled()) {
                            submitCode();
                        }
                    }
                });
    }

    private void setupEditPhoneNumberTextView() {
        mPhoneTextView.setText(mPhoneNumber);
        mPhoneTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });
    }

    private void setupResendConfirmationCodeTextView() {
        mResendCodeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.verifyPhoneNumber(mPhoneNumber, true);

                mResendCodeTextView.setVisibility(View.GONE);
                mCountDownTextView.setVisibility(View.VISIBLE);
                mCountDownTextView.setText(String.format(getString(R.string.fui_resend_code_in),
                        RESEND_WAIT_MILLIS / 1000));
                mMillisUntilFinished = RESEND_WAIT_MILLIS;
                mLooper.postDelayed(mCountdown, TICK_INTERVAL_MILLIS);
            }
        });
    }

    private void processCountdownTick() {
        mMillisUntilFinished -= TICK_INTERVAL_MILLIS;
        if (mMillisUntilFinished <= 0) {
            mCountDownTextView.setText("");
            mCountDownTextView.setVisibility(View.GONE);
            mResendCodeTextView.setVisibility(View.VISIBLE);
        } else {
            mCountDownTextView.setText(String.format(getString(R.string.fui_resend_code_in),
                    TimeUnit.MILLISECONDS.toSeconds(mMillisUntilFinished) + 1));
            mLooper.postDelayed(mCountdown, TICK_INTERVAL_MILLIS);
        }
    }

    private void submitCode() {
        mHandler.submitVerificationCode(
                mPhoneNumber, mConfirmationCodeEditText.getUnspacedText().toString());
    }

    @Override
    public void showProgress(int message) {
        mSubmitConfirmationButton.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        mSubmitConfirmationButton.setEnabled(true);
        mProgressBar.setVisibility(View.INVISIBLE);
    }
}
