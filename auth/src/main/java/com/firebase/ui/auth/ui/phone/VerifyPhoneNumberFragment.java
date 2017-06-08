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
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
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
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.GoogleApiHelper;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Locale;

/**
 * Displays country selector and phone number input form for users
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VerifyPhoneNumberFragment extends FragmentBase implements View.OnClickListener {
    public static final String TAG = "VerifyPhoneFragment";

    private CountryListSpinner countryListSpinner;
    private EditText mPhoneEditText;
    private TextView errorEditText;
    private Button sendCodeButton;
    private PhoneVerificationActivity mVerifier;
    private TextView mSmsTermsText;

    private static final int RC_PHONE_HINT = 22;

    public static VerifyPhoneNumberFragment newInstance(FlowParameters flowParameters,
                                                        String phone) {
        VerifyPhoneNumberFragment fragment = new VerifyPhoneNumberFragment();

        Bundle args = new Bundle();
        args.putParcelable(ExtraConstants.EXTRA_FLOW_PARAMS, flowParameters);
        args.putString(ExtraConstants.EXTRA_PHONE, phone);

        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.phone_layout, container, false);

        countryListSpinner = (CountryListSpinner) v.findViewById(R.id.country_list);
        mPhoneEditText = (EditText) v.findViewById(R.id.phone_number);
        errorEditText = (TextView) v.findViewById(R.id.phone_number_error);
        sendCodeButton = (Button) v.findViewById(R.id.send_code);
        mSmsTermsText = (TextView) v.findViewById(R.id.send_sms_tos);

        FragmentActivity parentActivity = getActivity();
        parentActivity.setTitle(getString(R.string.verify_phone_number_title));
        setUpCountrySpinner();
        setupSendCodeButton();
        setupTerms();

        return v;
    }

    private void setupTerms() {
        final String verifyPhoneButtonText = getString(R.string.verify_phone_number);
        final String terms = getString(R.string.sms_terms_of_service, verifyPhoneButtonText);
        mSmsTermsText.setText(terms);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Set listener
        if (!(getActivity() instanceof PhoneVerificationActivity)) {
            throw new IllegalStateException("Activity must implement PhoneVerificationHandler");
        }
        mVerifier = (PhoneVerificationActivity) getActivity();

        if (savedInstanceState != null) {
            return;
        }

        // Check for phone
        // It is assumed that the phone number that are being wired in via Credential Selector
        // are e164 since we store it.
        String phone = getArguments().getString(ExtraConstants.EXTRA_PHONE);
        if (!TextUtils.isEmpty(phone)) {
            // Use phone passed in
            PhoneNumber phoneNumber = PhoneNumberUtils.getPhoneNumber(phone);
            setPhoneNumber(phoneNumber);
            setCountryCode(phoneNumber);
        } else if (mHelper.getFlowParams().enableHints) {
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
                            PhoneNumberUtils
                                    .formatPhoneNumberUsingCurrentCountry(unformattedPhone,
                                                                          getContext());
                    if (formattedPhone == null) {
                        Log.e(TAG, "Unable to normalize phone number from hint selector:"
                                + unformattedPhone);
                        return;
                    }
                    final PhoneNumber phoneNumberObj =
                            PhoneNumberUtils.getPhoneNumber(formattedPhone);
                    setPhoneNumber(phoneNumberObj);
                    setCountryCode(phoneNumberObj);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        String phoneNumber = getPseudoValidPhoneNumber();
        if (phoneNumber == null) {
            errorEditText.setText(R.string.invalid_phone_number);
        } else {
            mVerifier.verifyPhoneNumber(phoneNumber, false);
        }
    }

    @Nullable
    String getPseudoValidPhoneNumber() {
        final CountryInfo countryInfo = (CountryInfo) countryListSpinner.getTag();
        final String everythingElse = mPhoneEditText.getText().toString();

        if (TextUtils.isEmpty(everythingElse)) {
            return null;
        }

        return PhoneNumberUtils.formatPhoneNumber(everythingElse, countryInfo);
    }

    private void setUpCountrySpinner() {
        //clear error when spinner is clicked on
        countryListSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorEditText.setText("");
            }
        });
    }

    private void setupSendCodeButton() {
        sendCodeButton.setOnClickListener(this);
    }

    private void showPhoneAutoCompleteHint() {
        try {
            mHelper.startIntentSenderForResult(getPhoneHintIntent().getIntentSender(),
                                               RC_PHONE_HINT);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Unable to start hint intent", e);
        }
    }

    private PendingIntent getPhoneHintIntent() {
        GoogleApiClient client = new GoogleApiClient.Builder(getContext()).addApi(Auth
                .CREDENTIALS_API).enableAutoManage(getActivity(), GoogleApiHelper
                .getSafeAutoManageId(), new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                Log.e(TAG, "Client connection failed: " + connectionResult.getErrorMessage());
            }
        }).build();


        HintRequest hintRequest = new HintRequest.Builder().setHintPickerConfig(new
                CredentialPickerConfig.Builder().setShowCancelButton(true).build())
                .setPhoneNumberIdentifierSupported(true).setEmailAddressIdentifierSupported
                        (false).build();

        return Auth.CredentialsApi.getHintPickerIntent(client, hintRequest);
    }

    private void setPhoneNumber(PhoneNumber phoneNumber) {
        if (PhoneNumber.isValid(phoneNumber)) {
            mPhoneEditText.setText(phoneNumber.getPhoneNumber());
            mPhoneEditText.setSelection(phoneNumber.getPhoneNumber().length());
        }
    }

    private void setCountryCode(PhoneNumber phoneNumber) {
        if (PhoneNumber.isCountryValid(phoneNumber)) {
            countryListSpinner.setSelectedForCountry(new Locale("", phoneNumber.getCountryIso()),
                    phoneNumber.getCountryCode());
        }
    }

    void showError(String e) {
        errorEditText.setText(e);
    }
}
