package com.firebase.ui.auth.ui.phone;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.PhoneNumber;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.ViewModelProvider;

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
    private View mCountryListAnchor;
    private TextInputLayout mPhoneInputLayout;
    private EditText mPhoneEditText;
    private TextView mSmsTermsText;
    private TextView mFooterText;


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
        mVerificationHandler = new ViewModelProvider(requireActivity())
                .get(PhoneNumberVerificationHandler.class);
        mCheckPhoneHandler = new ViewModelProvider(this)
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
        mCountryListAnchor = view.findViewById(R.id.country_list_popup_anchor);
        mPhoneInputLayout = view.findViewById(R.id.phone_layout);
        mPhoneEditText = view.findViewById(R.id.phone_number);
        mSmsTermsText = view.findViewById(R.id.send_sms_tos);
        mFooterText = view.findViewById(R.id.email_footer_tos_and_pp_text);

        mSmsTermsText.setText(getString(R.string.fui_sms_terms_of_service,
                getString(R.string.fui_verify_phone_number)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getFlowParams().enableHints) {
            mPhoneEditText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        requireActivity().setTitle(getString(R.string.fui_verify_phone_number_title));

        ImeHelper.setImeOnDoneListener(mPhoneEditText, () -> onNext());
        mSubmitButton.setOnClickListener(this);

        setupPrivacyDisclosures();
        setupCountrySpinner();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCheckPhoneHandler.getOperation().observe(getViewLifecycleOwner(), new ResourceObserver<PhoneNumber>(this) {
            @Override
            protected void onSuccess(@NonNull PhoneNumber number) {
                start(number);
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                // Just let the user enter their data
            }
        });

        if (savedInstanceState != null || mCalled) {
            return;
        }
        // Fragment back stacks are the stuff of nightmares (what's new?): the fragment isn't
        // destroyed so its state isn't saved and we have to rely on an instance field. Sigh.
        mCalled = true;

        // DON'T REMOVE
        setDefaultCountryForSpinner();
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

        if (PhoneNumber.isCountryValid(number) && mCountryListSpinner.isValidIso(iso)) {
            setCountryCode(number);
            onNext();
        }
    }

    private void onNext() {
        String phoneNumber = getPseudoValidPhoneNumber();
        if (phoneNumber == null) {
            mPhoneInputLayout.setError(getString(R.string.fui_invalid_phone_number));
        } else {
            mVerificationHandler.verifyPhoneNumber(requireActivity(), phoneNumber, false);
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

    private void setupPrivacyDisclosures() {
        FlowParameters params = getFlowParams();

        boolean termsAndPrivacyUrlsProvided = params.isTermsOfServiceUrlProvided()
                && params.isPrivacyPolicyUrlProvided();

        if (!params.shouldShowProviderChoice() && termsAndPrivacyUrlsProvided) {
            PrivacyDisclosureUtils.setupTermsOfServiceAndPrivacyPolicySmsText(requireContext(),
                    params,
                    mSmsTermsText);
        } else {
            PrivacyDisclosureUtils.setupTermsOfServiceFooter(requireContext(),
                    params,
                    mFooterText);

            String verifyText = getString(R.string.fui_verify_phone_number);
            mSmsTermsText.setText(getString(R.string.fui_sms_terms_of_service, verifyText));
        }
    }

    private void setCountryCode(PhoneNumber number) {
        mCountryListSpinner.setSelectedForCountry(
                new Locale("", number.getCountryIso()), number.getCountryCode());
    }

    private void setupCountrySpinner() {
        Bundle params = getArguments().getBundle(ExtraConstants.PARAMS);
        mCountryListSpinner.init(params, mCountryListAnchor);

        // Clear error when spinner is clicked on
        mCountryListSpinner.setOnClickListener(v -> mPhoneInputLayout.setError(null));
    }

    private void setDefaultCountryForSpinner() {
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
        } else if (getFlowParams().enableHints) {
            mCheckPhoneHandler.fetchCredential();
        }
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
