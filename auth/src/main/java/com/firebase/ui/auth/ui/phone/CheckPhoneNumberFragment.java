package com.firebase.ui.auth.ui.phone;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.CountryInfo;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.PhoneNumber;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.viewmodel.ResourceObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Displays country selector and phone number input form for users
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CheckPhoneNumberFragment extends FragmentBase implements View.OnClickListener {
    public static final String TAG = "VerifyPhoneFragment";

    private PhoneNumberVerificationHandler mVerificationHandler;
    private CheckPhoneHandler mCheckPhoneHandler;
    private boolean mCalled;

    private ProgressBar mProgressBar;
    private Button mSubmitButton;
    private CountryListSpinner mCountryListSpinner;
    private TextInputLayout mPhoneInputLayout;
    private EditText mPhoneEditText;
    private TextView mSmsTermsText;

    private Set<String> mWhitelistedCountryIsos;
    private Set<String> mBlacklistedCountryIsos;

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
        mVerificationHandler = ViewModelProviders.of(requireActivity())
                .get(PhoneNumberVerificationHandler.class);
        mCheckPhoneHandler = ViewModelProviders.of(requireActivity())
                .get(CheckPhoneHandler.class);
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
        mProgressBar = view.findViewById(R.id.top_progress_bar);
        mSubmitButton = view.findViewById(R.id.send_code);
        mCountryListSpinner = view.findViewById(R.id.country_list);
        mPhoneInputLayout = view.findViewById(R.id.phone_layout);
        mPhoneEditText = view.findViewById(R.id.phone_number);
        mSmsTermsText = view.findViewById(R.id.send_sms_tos);

        mSmsTermsText.setText(getString(R.string.fui_sms_terms_of_service,
                getString(R.string.fui_verify_phone_number)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getFlowParams().enableHints) {
            mPhoneEditText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        requireActivity().setTitle(getString(R.string.fui_verify_phone_number_title));

        ImeHelper.setImeOnDoneListener(mPhoneEditText, new ImeHelper.DonePressedListener() {
            @Override
            public void onDonePressed() {
                onNext();
            }
        });
        mSubmitButton.setOnClickListener(this);

        setupCountrySpinner();
        setupPrivacyDisclosures(view.<TextView>findViewById(R.id.email_footer_tos_and_pp_text));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCheckPhoneHandler.getOperation().observe(this, new ResourceObserver<PhoneNumber>(this) {
            @Override
            protected void onSuccess(@NonNull PhoneNumber number) {
                start(number);
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                // Just let the user enter their data
            }
        });

        if (savedInstanceState != null || mCalled) { return; }
        // Fragment back stacks are the stuff of nightmares (what's new?): the fragment isn't
        // destroyed so its state isn't saved and we have to rely on an instance field. Sigh.
        mCalled = true;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mCheckPhoneHandler.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        onNext();
    }

    private void start(PhoneNumber number) {
        if (!PhoneNumber.isValid(number)) {
            mPhoneInputLayout.setError(getString(R.string.fui_invalid_phone_number));
            return;
        }
        mPhoneEditText.setText(number.getPhoneNumber());
        mPhoneEditText.setSelection(number.getPhoneNumber().length());

        String iso = number.getCountryIso();

        if (PhoneNumber.isCountryValid(number) && isValidIsoBasedOnCountrySelectorConfig(iso)) {
            setCountryCode(number);
            onNext();
        }
    }

    private void onNext() {
        String phoneNumber = getPseudoValidPhoneNumber();
        if (phoneNumber == null) {
            mPhoneInputLayout.setError(getString(R.string.fui_invalid_phone_number));
        } else {
            mVerificationHandler.verifyPhoneNumber(phoneNumber, false);
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

    private void setupPrivacyDisclosures(TextView footerText) {
        FlowParameters params = getFlowParams();

        if (params.isSingleProviderFlow()) {
            PrivacyDisclosureUtils.setupTermsOfServiceAndPrivacyPolicySmsText(requireContext(),
                    params,
                    mSmsTermsText);
        } else {
            PrivacyDisclosureUtils.setupTermsOfServiceFooter(requireContext(),
                    params,
                    footerText);

            String verifyText = getString(R.string.fui_verify_phone_number);
            mSmsTermsText.setText(getString(R.string.fui_sms_terms_of_service, verifyText));
        }
    }

    private void setCountryCode(PhoneNumber number) {
        String iso = number.getCountryIso();
        if (isValidIsoBasedOnCountrySelectorConfig(iso)) {
            mCountryListSpinner.setSelectedForCountry(
                    new Locale("", iso), number.getCountryCode());
        }
    }

    private boolean isValidIsoBasedOnCountrySelectorConfig(String iso) {
        iso = iso.toUpperCase(Locale.getDefault());
        return ((mWhitelistedCountryIsos == null && mBlacklistedCountryIsos == null)
                || (mWhitelistedCountryIsos != null && mWhitelistedCountryIsos.contains(iso))
                || (mBlacklistedCountryIsos != null && !mBlacklistedCountryIsos.contains(iso)));
    }

    private void setupCountrySpinner() {
        getCountrySpinnerIsosFromParams();

        List<CountryInfo> countryInfoList = getCountriesToDisplayInSpinner();
        Collections.sort(countryInfoList);

        mCountryListSpinner.setCountryInfoList(countryInfoList);

        setDefaultCountryForSpinner(countryInfoList);

        // Clear error when spinner is clicked on
        mCountryListSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhoneInputLayout.setError(null);
            }
        });
    }

    private void setDefaultCountryForSpinner(List<CountryInfo> countryInfoList) {
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

        // We can receive the phone number in one of two formats: split between the ISO or fully
        // processed. If it's complete, we use it directly. Otherwise, we parse the ISO and national
        // number combination or we just set the default ISO if there's no default number. If there
        // are no defaults at all, we prompt the user for a phone number through Smart Lock.
        if (!TextUtils.isEmpty(phone)) {
            start(PhoneNumberUtils.getPhoneNumber(phone));
        } else if (!TextUtils.isEmpty(countryIso) && !TextUtils.isEmpty(nationalNumber)) {
            start(PhoneNumberUtils.getPhoneNumber(countryIso, nationalNumber));
        } else if (!TextUtils.isEmpty(countryIso)) {
            setCountryCode(new PhoneNumber(
                    "",
                    countryIso,
                    String.valueOf(PhoneNumberUtils.getCountryCode(countryIso))));
        } else {
            CountryInfo countryInfo = PhoneNumberUtils.getCurrentCountryInfo(getContext());
            if (isValidIsoBasedOnCountrySelectorConfig(countryInfo.getLocale().getCountry())) {
                mCountryListSpinner.setSelectedForCountry(countryInfo.getCountryCode(),
                        countryInfo.getLocale());
            } else {
                if (countryInfoList.iterator().hasNext()) {
                    countryInfo = countryInfoList.iterator().next();
                    mCountryListSpinner.setSelectedForCountry(countryInfo.getCountryCode(),
                            countryInfo.getLocale());
                }
            }
            if (getFlowParams().enableHints) {
                mCheckPhoneHandler.fetchCredential();
            }
        }
    }

    private void getCountrySpinnerIsosFromParams() {
        Bundle params = getArguments().getBundle(ExtraConstants.PARAMS);
        if (params != null) {
            List<String> whitelistedCountries =
                    params.getStringArrayList(ExtraConstants.WHITELISTED_COUNTRIES);
            List<String> blacklistedCountries =
                    params.getStringArrayList(ExtraConstants.BLACKLISTED_COUNTRIES);

            if (whitelistedCountries != null) {
                this.mWhitelistedCountryIsos = convertCodesToIsos(whitelistedCountries);
            } else if (blacklistedCountries != null) {
                this.mBlacklistedCountryIsos = convertCodesToIsos(blacklistedCountries);
            }
        }
    }

    private Set<String> convertCodesToIsos(@NonNull List<String> codes) {
        Set<String> isos = new HashSet<>();
        for (String code : codes) {
            if (PhoneNumberUtils.isValid(code)) {
                isos.addAll(PhoneNumberUtils.getCountryIsosFromCountryCode(code));
            } else {
                isos.add(code);
            }
        }
        return isos;
    }

    public List<CountryInfo> getCountriesToDisplayInSpinner() {
        Map<String, Integer> countryInfoMap = PhoneNumberUtils.getImmutableCountryIsoMap();
        // We consider all countries to be whitelisted if there are no whitelisted
        // or blacklisted countries given as input.
        if (mWhitelistedCountryIsos == null && mBlacklistedCountryIsos == null) {
            this.mWhitelistedCountryIsos = new HashSet<>(countryInfoMap.keySet());
        }

        List<CountryInfo> countryInfoList = new ArrayList<>();

        // At this point either mWhitelistedCountryIsos or mBlacklistedCountryIsos is null.
        // We assume no countries are to be excluded. Here, we correct this assumption based on the
        // contents of either lists.
        Set<String> excludedCountries = new HashSet<>();
        if (mWhitelistedCountryIsos == null) {
            // Exclude all countries in the mBlacklistedCountryIsos list.
            excludedCountries.addAll(mBlacklistedCountryIsos);
        } else {
            // Exclude all countries that are not present in the mWhitelistedCountryIsos list.
            excludedCountries.addAll(countryInfoMap.keySet());
            excludedCountries.removeAll(mWhitelistedCountryIsos);
        }

        // Once we know which countries need to be excluded, we loop through the country isos,
        // skipping those that have been excluded.
        for (String countryIso : countryInfoMap.keySet()) {
            if (!excludedCountries.contains(countryIso)) {
                countryInfoList.add(new CountryInfo(new Locale("", countryIso),
                        countryInfoMap.get(countryIso)));
            }
        }

        return countryInfoList;
    }

    @Override
    public void showProgress(int message) {
        mSubmitButton.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        mSubmitButton.setEnabled(true);
        mProgressBar.setVisibility(View.INVISIBLE);
    }
}
