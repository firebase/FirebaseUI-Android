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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.util.ui.BucketedTextChangeListener;
import com.firebase.ui.auth.viewmodel.phone.PhoneProviderResponseHandler;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

/**
 * Display confirmation code to verify phone numbers input in {@link CheckPhoneNumberFragment}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SubmitConfirmationCodeFragment extends FragmentBase {

    public static final String TAG = "SubmitConfirmationCodeFragment";

    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final long RESEND_WAIT_MILLIS = 60000;
    private static final long TICK_INTERVAL_MILLIS = 500;
    private static final String EXTRA_MILLIS_UNTIL_FINISHED = "millis_until_finished";

    private final Handler mLooper = new Handler();
    private final Runnable mCountdown = () -> processCountdownTick();

    private PhoneNumberVerificationHandler mHandler;
    private String mPhoneNumber;

    private ProgressBar mProgressBar;
    private TextView mPhoneTextView;
    private TextView mResendCodeTextView;
    private TextView mCountDownTextView;
    private SpacedEditText mConfirmationCodeEditText;
    private long mMillisUntilFinished = RESEND_WAIT_MILLIS;

    private boolean mHasResumed;

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
        mHandler = new ViewModelProvider(requireActivity())
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

        requireActivity().setTitle(getString(R.string.fui_verify_your_phone_title));
        processCountdownTick();
        setupConfirmationCodeEditText();
        setupEditPhoneNumberTextView();
        setupResendConfirmationCodeTextView();
        PrivacyDisclosureUtils.setupTermsOfServiceFooter(
                requireContext(),
                getFlowParams(),
                view.findViewById(R.id.email_footer_tos_and_pp_text));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        new ViewModelProvider(requireActivity())
                .get(PhoneProviderResponseHandler.class)
                .getOperation()
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource.getState() == State.FAILURE) {
                        mConfirmationCodeEditText.setText("");
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        mConfirmationCodeEditText.requestFocus();
        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(mConfirmationCodeEditText, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mHasResumed) {
            // Don't check for codes before we've even had the chance to send one.
            mHasResumed = true;
            return;
        }

        ClipData clip = ContextCompat.getSystemService(requireContext(), ClipboardManager.class)
                .getPrimaryClip();
        if (clip != null && clip.getItemCount() == 1) {
            CharSequence candidate = clip.getItemAt(0).getText();
            if (candidate != null && candidate.length() == VERIFICATION_CODE_LENGTH) {
                try {
                    Integer.parseInt(candidate.toString());

                    // We have a number! Try to submit it.
                    mConfirmationCodeEditText.setText(candidate);
                } catch (NumberFormatException ignored) {
                    // Turns out it wasn't a number
                }
            }
        }

        mLooper.removeCallbacks(mCountdown);
        mLooper.postDelayed(mCountdown, TICK_INTERVAL_MILLIS);
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

    private void setupConfirmationCodeEditText() {
        mConfirmationCodeEditText.setText("------");
        mConfirmationCodeEditText.addTextChangedListener(new BucketedTextChangeListener(
                mConfirmationCodeEditText, VERIFICATION_CODE_LENGTH, "-",
                new BucketedTextChangeListener.ContentChangeCallback() {
                    @Override
                    public void whenComplete() {
                        submitCode();
                    }

                    @Override
                    public void whileIncomplete() {}
                }));
    }

    private void setupEditPhoneNumberTextView() {
        mPhoneTextView.setText(mPhoneNumber);
        mPhoneTextView.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void setupResendConfirmationCodeTextView() {
        mResendCodeTextView.setOnClickListener(v -> {
            mHandler.verifyPhoneNumber(requireActivity(), mPhoneNumber, true);

            mResendCodeTextView.setVisibility(View.GONE);
            mCountDownTextView.setVisibility(View.VISIBLE);
            mCountDownTextView.setText(String.format(getString(R.string.fui_resend_code_in),
                    RESEND_WAIT_MILLIS / 1000));
            mMillisUntilFinished = RESEND_WAIT_MILLIS;
            mLooper.postDelayed(mCountdown, TICK_INTERVAL_MILLIS);
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
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        mProgressBar.setVisibility(View.INVISIBLE);
    }
}
