package com.firebase.ui.auth.ui.phone;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.PhoneNumber;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.firebase.ui.auth.util.ui.FlowUtils;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.HintRequest;

import java.util.Locale;

/**
 * Displays country selector and phone number input form for users
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CheckPhoneNumberFragment extends FragmentBase implements View.OnClickListener {
    public static final String TAG = "VerifyPhoneFragment";

    private PhoneNumberVerificationHandler mHandler;
    private boolean mCalled;

    private CountryListSpinner mCountryListSpinner;
    private TextInputLayout mPhoneInputLayout;
    private EditText mPhoneEditText;
    private TextView mSmsTermsText;

    public static CheckPhoneNumberFragment newInstance(Bundle params) {
        CheckPhoneNumberFragment fragment = new CheckPhoneNumberFragment();
        Bundle args = new Bundle();
        args.putBundle(ExtraConstants.PARAMS, params);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = ViewModelProviders.of(getActivity()).get(PhoneNumberVerificationHandler.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fui_phone_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mCountryListSpinner = view.findViewById(R.id.country_list);
        mPhoneInputLayout = view.findViewById(R.id.phone_layout);
        mPhoneEditText = view.findViewById(R.id.phone_number);
        mSmsTermsText = view.findViewById(R.id.send_sms_tos);

        mSmsTermsText.setText(getString(R.string.fui_sms_terms_of_service,
                getString(R.string.fui_verify_phone_number)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getFlowParams().enableHints) {
            mPhoneEditText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        getActivity().setTitle(getString(R.string.fui_verify_phone_number_title));

        ImeHelper.setImeOnDoneListener(mPhoneEditText, new ImeHelper.DonePressedListener() {
            @Override
            public void onDonePressed() {
                onNext();
            }
        });
        view.findViewById(R.id.send_code).setOnClickListener(this);
        mCountryListSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhoneInputLayout.setError(null);
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null || mCalled) { return; }
        // Fragment back stacks are the stuff of nightmares (what's new?): the fragment isn't
        // destroyed so its state isn't saved and we have to rely on an instance field. Sigh.
        mCalled = true;

        // Check for phone
        // It is assumed that the phone number that are being wired in via Credential Selector
        // are e164 since we store it.
        Bundle params = getArguments().getBundle(ExtraConstants.PARAMS);
        String phone = null;
        String countryIso = null;
        String nationalNumber = null;
        if (params != null) {
            phone = params.getString(ExtraConstants.PHONE);
            countryIso = params.getString(ExtraConstants.COUNTRY_ISO);
            nationalNumber = params.getString(ExtraConstants.NATIONAL_NUMBER);
        }

        if (!TextUtils.isEmpty(countryIso) && !TextUtils.isEmpty(nationalNumber)) {
            start(PhoneNumberUtils.getPhoneNumber(countryIso, nationalNumber));
        } else if (!TextUtils.isEmpty(countryIso)) {
            setCountryCode(new PhoneNumber(
                    "",
                    countryIso,
                    String.valueOf(PhoneNumberUtils.getCountryCode(countryIso))));
        } else if (!TextUtils.isEmpty(phone)) {
            start(PhoneNumberUtils.getPhoneNumber(phone));
        } else if (getFlowParams().enableHints) {
            FlowUtils.unhandled(this, new PendingIntentRequiredException(
                    Credentials.getClient(mHandler.getApplication()).getHintPickerIntent(
                            new HintRequest.Builder().setPhoneNumberIdentifierSupported(true)
                                    .build()),
                    RequestCodes.CRED_HINT));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != RequestCodes.CRED_HINT || resultCode != Activity.RESULT_OK) { return; }

        Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
        String formattedPhone = PhoneNumberUtils.formatUsingCurrentCountry(
                credential.getId(), getContext());
        if (formattedPhone != null) {
            start(PhoneNumberUtils.getPhoneNumber(formattedPhone));
        }
    }

    @Override
    public void onClick(View v) {
        onNext();
    }

    private void start(PhoneNumber number) {
        if (PhoneNumber.isValid(number)) {
            mPhoneEditText.setText(number.getPhoneNumber());
            mPhoneEditText.setSelection(number.getPhoneNumber().length());
        }
        if (PhoneNumber.isCountryValid(number)) {
            setCountryCode(number);
        }

        onNext();
    }

    private void onNext() {
        String phoneNumber = getPseudoValidPhoneNumber();
        if (phoneNumber == null) {
            mPhoneInputLayout.setError(getString(R.string.fui_invalid_phone_number));
        } else {
            mHandler.verifyPhoneNumber(phoneNumber, false);
        }
    }

    @Nullable
    private String getPseudoValidPhoneNumber() {
        String everythingElse = mPhoneEditText.getText().toString();

        if (TextUtils.isEmpty(everythingElse)) {
            return null;
        }

        return PhoneNumberUtils.format(
                everythingElse, mCountryListSpinner.getSelectedCountryInfo());
    }

    private void setCountryCode(PhoneNumber number) {
        mCountryListSpinner.setSelectedForCountry(
                new Locale("", number.getCountryIso()), number.getCountryCode());
    }

    public String getPhoneNumber() {
        return mPhoneEditText.getText().toString();
    }
}
