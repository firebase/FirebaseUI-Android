package com.firebase.ui.auth.ui.phone;

import android.arch.lifecycle.ViewModelProviders;
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

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.CountryInfo;
import com.firebase.ui.auth.data.model.PhoneNumber;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.viewmodel.ResourceObserver;

import java.util.Locale;

/**
 * Displays country selector and phone number input form for users
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CheckPhoneNumberFragment extends FragmentBase implements View.OnClickListener {
    public static final String TAG = "VerifyPhoneFragment";

    private CheckPhoneNumberHandler mHandler;

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
        mHandler = ViewModelProviders.of(getActivity()).get(CheckPhoneNumberHandler.class);
        mHandler.getPhoneNumberListener().observe(this, new ResourceObserver<PhoneNumber>(
                this, R.string.fui_progress_dialog_checking_accounts) {
            @Override
            protected void onSuccess(@NonNull PhoneNumber number) {
                start(number);
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                // Just let the user enter their data
            }
        });
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getFlowParams().enableHints) {
            mPhoneEditText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        ImeHelper.setImeOnDoneListener(mPhoneEditText, new ImeHelper.DonePressedListener() {
            @Override
            public void onDonePressed() {
                onNext();
            }
        });

        getActivity().setTitle(getString(R.string.fui_verify_phone_number_title));
        view.findViewById(R.id.send_code).setOnClickListener(this);
        setupCountrySpinner();
        setupTerms();
    }

    private void setupCountrySpinner() {
        mCountryListSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhoneInputLayout.setError(null);
            }
        });
    }

    private void setupTerms() {
        String verifyPhoneButtonText = getString(R.string.fui_verify_phone_number);
        String terms = getString(R.string.fui_sms_terms_of_service, verifyPhoneButtonText);
        mSmsTermsText.setText(terms);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) { return; }

        // Check for phone
        // It is assumed that the phone number that are being wired in via Credential Selector
        // are e164 since we store it.
        Bundle params = getArguments().getBundle(ExtraConstants.PARAMS);
        String phone = null;
        String countryCode = null;
        String nationalNumber = null;
        if (params != null) {
            phone = params.getString(AuthUI.EXTRA_DEFAULT_PHONE_NUMBER);
            countryCode = params.getString(AuthUI.EXTRA_DEFAULT_COUNTRY_CODE);
            nationalNumber = params.getString(AuthUI.EXTRA_DEFAULT_NATIONAL_NUMBER);

            // Clear these args so the user doesn't get stuck being sent to the submit code fragment
            params.remove(AuthUI.EXTRA_DEFAULT_PHONE_NUMBER);
            params.remove(AuthUI.EXTRA_DEFAULT_COUNTRY_CODE);
            params.remove(AuthUI.EXTRA_DEFAULT_NATIONAL_NUMBER);
        }

        if (!TextUtils.isEmpty(countryCode) && !TextUtils.isEmpty(nationalNumber)) {
            start(PhoneNumberUtils.getPhoneNumber(countryCode, nationalNumber));
        } else if (!TextUtils.isEmpty(phone)) {
            start(PhoneNumberUtils.getPhoneNumber(phone));
        } else if (getFlowParams().enableHints) {
            mHandler.fetchCredential();
        }
    }

    private void start(PhoneNumber number) {
        if (PhoneNumber.isValid(number)) {
            mPhoneEditText.setText(number.getPhoneNumber());
            mPhoneEditText.setSelection(number.getPhoneNumber().length());
        }
        if (PhoneNumber.isCountryValid(number)) {
            mCountryListSpinner.setSelectedForCountry(
                    new Locale("", number.getCountryIso()), number.getCountryCode());
        }

        onNext();
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
            mHandler.verifyPhoneNumber(phoneNumber, false);
        }
    }

    @Nullable
    private String getPseudoValidPhoneNumber() {
        CountryInfo countryInfo = (CountryInfo) mCountryListSpinner.getTag();
        String everythingElse = mPhoneEditText.getText().toString();

        if (TextUtils.isEmpty(everythingElse)) {
            return null;
        }

        return PhoneNumberUtils.format(everythingElse, countryInfo);
    }
}
