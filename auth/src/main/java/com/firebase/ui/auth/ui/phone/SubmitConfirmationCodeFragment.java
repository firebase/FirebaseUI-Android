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
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.CustomCountDownTimer;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.ui.BucketedTextChangeListener;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.util.ui.PreambleHandler;

import java.util.concurrent.TimeUnit;

/**
 * Display confirmation code to verify phone numbers input in {{@link CheckPhoneNumberFragment}}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SubmitConfirmationCodeFragment extends FragmentBase {

    public static final String TAG = "SubmitConfirmationCodeFragment";

    private static final long RESEND_WAIT_MILLIS = 15000;
    private static final String EXTRA_MILLIS_UNTIL_FINISHED = "millis_until_finished";

    private CheckPhoneNumberHandler mHandler;
    private String mPhoneNumber;

    private TextView mEditPhoneTextView;
    private TextView mResendCodeTextView;
    private TextView mCountDownTextView;
    private SpacedEditText mConfirmationCodeEditText;
    private Button mSubmitConfirmationButton;
    private CustomCountDownTimer mCountdownTimer;
    private long mMillisUntilFinished;

    public static SubmitConfirmationCodeFragment newInstance(String phoneNumber) {
        SubmitConfirmationCodeFragment fragment = new SubmitConfirmationCodeFragment();
        Bundle args = new Bundle();
        args.putString(ExtraConstants.EXTRA_PHONE, phoneNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = ViewModelProviders.of(getActivity()).get(CheckPhoneNumberHandler.class);
        mPhoneNumber = getArguments().getString(ExtraConstants.EXTRA_PHONE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fui_confirmation_code_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mEditPhoneTextView = view.findViewById(R.id.edit_phone_number);
        mCountDownTextView = view.findViewById(R.id.ticker);
        mResendCodeTextView = view.findViewById(R.id.resend_code);
        mConfirmationCodeEditText = view.findViewById(R.id.confirmation_code);
        mSubmitConfirmationButton = view.findViewById(R.id.submit_confirmation_code);

        getActivity().setTitle(getString(R.string.fui_verify_your_phone_title));
        mSubmitConfirmationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCode();
            }
        });
        setupConfirmationCodeEditText();
        setupEditPhoneNumberTextView();
        setupResendConfirmationCodeTextView();
        setupCountDown(RESEND_WAIT_MILLIS);
        PreambleHandler.setup(
                getContext(),
                getFlowHolder().getParams(),
                R.string.fui_continue_phone_login,
                view.<TextView>findViewById(R.id.create_account_tos));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mCountdownTimer.update(savedInstanceState.getLong(EXTRA_MILLIS_UNTIL_FINISHED));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mConfirmationCodeEditText.requestFocus();
        ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(mConfirmationCodeEditText, 0);
    }

    @Override
    public void onDestroy() {
        mCountdownTimer.cancel();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_MILLIS_UNTIL_FINISHED, mMillisUntilFinished);
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
        mEditPhoneTextView.setText(mPhoneNumber);
        mEditPhoneTextView.setOnClickListener(new View.OnClickListener() {
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
                mCountdownTimer.renew();
            }
        });
    }

    private void setupCountDown(long startTimeMillis) {
        mCountdownTimer = new CustomCountDownTimer(startTimeMillis, 500) {
            @Override
            protected void onTick(long millisUntilFinished) {
                mMillisUntilFinished = millisUntilFinished;
                mCountDownTextView.setText(String.format(getString(R.string.fui_resend_code_in),
                        TimeUnit.MILLISECONDS.toSeconds(mMillisUntilFinished)));
            }

            @Override
            protected void onFinish() {
                mCountDownTextView.setText("");
                mCountDownTextView.setVisibility(View.GONE);
                mResendCodeTextView.setVisibility(View.VISIBLE);
            }
        };

        mCountdownTimer.start();
    }

    private void submitCode() {
        mHandler.submitVerificationCode(mConfirmationCodeEditText.getUnspacedText().toString());
    }
}
