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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.CountryInfo;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.PhoneNumber;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.HintRequest;

import java.util.Locale;

/**
 * Displays country selector and phone number input form for users
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VerifyPhoneNumberFragment extends FragmentBase implements View.OnClickListener {
    public static final String TAG = "VerifyPhoneFragment";
    private static final int RC_PHONE_HINT = 22;

    private Context mAppContext;

    private CountryListSpinner mCountryListSpinner;
    private TextInputLayout mPhoneInputLayout;
    private EditText mPhoneEditText;
    private Button mSendCodeButton;
    private PhoneActivity mVerifier;
    private TextView mSmsTermsText;

    public static VerifyPhoneNumberFragment newInstance(
            FlowParameters flowParameters, Bundle params) {
        VerifyPhoneNumberFragment fragment = new VerifyPhoneNumberFragment();

        Bundle args = new Bundle();
        args.putParcelable(ExtraConstants.EXTRA_FLOW_PARAMS, flowParameters);
        args.putBundle(ExtraConstants.EXTRA_PARAMS, params);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAppContext = context.getApplicationContext();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fui_phone_layout, container, false);

        mCountryListSpinner = v.findViewById(R.id.country_list);
        mPhoneInputLayout = v.findViewById(R.id.phone_layout);
        mPhoneEditText = v.findViewById(R.id.phone_number);
        mSendCodeButton = v.findViewById(R.id.send_code);
        mSmsTermsText = v.findViewById(R.id.send_sms_tos);

        ImeHelper.setImeOnDoneListener(mPhoneEditText, new ImeHelper.DonePressedListener() {
            @Override
            public void onDonePressed() {
                onNext();
            }
        });

        FragmentActivity parentActivity = getActivity();
        parentActivity.setTitle(getString(R.string.fui_verify_phone_number_title));
        setupCountrySpinner();
        setupSendCodeButton();
        setupTerms();

        return v;
    }

    private void setupTerms() {
        final String verifyPhoneButtonText = getString(R.string.fui_verify_phone_number);
        final String terms = getString(R.string.fui_sms_terms_of_service, verifyPhoneButtonText);
        mSmsTermsText.setText(terms);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Set listener
        if (!(getActivity() instanceof PhoneActivity)) {
            throw new IllegalStateException("Activity must implement PhoneVerificationHandler");
        }
        mVerifier = (PhoneActivity) getActivity();

        if (savedInstanceState != null) {
            return;
        }

        // Check for phone
        // It is assumed that the phone number that are being wired in via Credential Selector
        // are e164 since we store it.
        Bundle params = getArguments().getBundle(ExtraConstants.EXTRA_PARAMS);
        String phone = null;
        String countryIso = null;
        String nationalNumber = null;
        if (params != null) {
            phone = params.getString(ExtraConstants.EXTRA_PHONE);
            countryIso = params.getString(ExtraConstants.EXTRA_COUNTRY_ISO);
            nationalNumber = params.getString(ExtraConstants.EXTRA_NATIONAL_NUMBER);
        }
        if (!TextUtils.isEmpty(countryIso) && !TextUtils.isEmpty(nationalNumber)) {
            // User supplied country code & national number
            PhoneNumber phoneNumber = PhoneNumberUtils.getPhoneNumber(countryIso, nationalNumber);
            setPhoneNumber(phoneNumber);
            setCountryCode(phoneNumber);
        } else if (!TextUtils.isEmpty(countryIso)) {
            setCountryCode(new PhoneNumber(
                    "",
                    countryIso,
                    String.valueOf(PhoneNumberUtils.getCountryCode(countryIso))));
        } else if (!TextUtils.isEmpty(phone)) {
            // User supplied full phone number
            PhoneNumber phoneNumber = PhoneNumberUtils.getPhoneNumber(phone);
            setPhoneNumber(phoneNumber);
            setCountryCode(phoneNumber);
        } else if (getFlowParams().enableHints) {
            // Try SmartLock phone autocomplete hint
            showPhoneAutoCompleteHint();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PHONE_HINT) {
            if (data != null) {
                Credential cred = data.getParcelableExtra(Credential.EXTRA_KEY);
                if (cred != null) {
                    // Hint selector does not always return phone numbers in e164 format.
                    // To accommodate either case, we normalize to e164 with best effort
                    final String unformattedPhone = cred.getId();
                    final String formattedPhone =
                            PhoneNumberUtils.formatUsingCurrentCountry(unformattedPhone,
                                    mAppContext);
                    if (formattedPhone == null) {
                        Log.e(TAG, "Unable to normalize phone number from hint selector:"
                                + unformattedPhone);
                        return;
                    }
                    final PhoneNumber phoneNumberObj =
                            PhoneNumberUtils.getPhoneNumber(formattedPhone);
                    setPhoneNumber(phoneNumberObj);
                    setCountryCode(phoneNumberObj);
                    onNext();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        onNext();
    }

    private void onNext() {
        String phoneNumber = getPseudoValidPhoneNumber();
        if (phoneNumber == null) {
            mPhoneInputLayout.setError(getString(R.string.fui_invalid_phone_number));
        } else {
            mVerifier.verifyPhoneNumber(phoneNumber, false);
        }
    }

    @Nullable
    private String getPseudoValidPhoneNumber() {
        final CountryInfo countryInfo = (CountryInfo) mCountryListSpinner.getTag();
        final String everythingElse = mPhoneEditText.getText().toString();

        if (TextUtils.isEmpty(everythingElse)) {
            return null;
        }

        return PhoneNumberUtils.format(everythingElse, countryInfo);
    }

    private void setupCountrySpinner() {
        //clear error when spinner is clicked on
        mCountryListSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhoneInputLayout.setError(null);
            }
        });
    }

    private void setupSendCodeButton() {
        mSendCodeButton.setOnClickListener(this);
    }

    private void showPhoneAutoCompleteHint() {
        try {
            startIntentSenderForResult(getPhoneHintIntent().getIntentSender(), RC_PHONE_HINT);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Unable to start hint intent", e);
        }
    }

    private PendingIntent getPhoneHintIntent() {
        HintRequest hintRequest = new HintRequest.Builder()
                .setHintPickerConfig(
                        new CredentialPickerConfig.Builder().setShowCancelButton(true).build())
                .setPhoneNumberIdentifierSupported(true)
                .setEmailAddressIdentifierSupported(false)
                .build();

        return GoogleApiUtils.getCredentialsClient(getContext()).getHintPickerIntent(hintRequest);
    }

    private void setPhoneNumber(PhoneNumber phoneNumber) {
        if (PhoneNumber.isValid(phoneNumber)) {
            mPhoneEditText.setText(phoneNumber.getPhoneNumber());
            mPhoneEditText.setSelection(phoneNumber.getPhoneNumber().length());
        }
    }

    private void setCountryCode(PhoneNumber phoneNumber) {
        if (PhoneNumber.isCountryValid(phoneNumber)) {
            mCountryListSpinner.setSelectedForCountry(new Locale("", phoneNumber.getCountryIso()),
                    phoneNumber.getCountryCode());
        }
    }

    void showError(String e) {
        mPhoneInputLayout.setError(e);
    }
}
