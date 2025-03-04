package com.firebase.ui.auth.ui.email;

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

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.util.ui.fieldvalidators.EmailFieldValidator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.EmailAuthProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import static com.firebase.ui.auth.AuthUI.EMAIL_LINK_PROVIDER;

/**
 * Fragment that shows a form with an email field and checks for existing accounts with that email.
 * <p>
 * Host Activities should implement {@link CheckEmailFragment.CheckEmailListener}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CheckEmailFragment extends FragmentBase implements
        View.OnClickListener,
        ImeHelper.DonePressedListener {

    public static final String TAG = "CheckEmailFragment";
    private CheckEmailHandler mHandler;
    private Button mSignInButton;
    private Button mSignUpButton;
    private ProgressBar mProgressBar;
    private EditText mEmailEditText;
    private TextInputLayout mEmailLayout;
    private EmailFieldValidator mEmailFieldValidator;
    private CheckEmailListener mListener;

    public static CheckEmailFragment newInstance(@Nullable String email) {
        CheckEmailFragment fragment = new CheckEmailFragment();
        Bundle args = new Bundle();
        args.putString(ExtraConstants.EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fui_check_email_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mSignInButton = view.findViewById(R.id.button_sign_in);
        mSignUpButton = view.findViewById(R.id.button_sign_up);
        mProgressBar = view.findViewById(R.id.top_progress_bar);

        mEmailLayout = view.findViewById(R.id.email_layout);
        mEmailEditText = view.findViewById(R.id.email);
        mEmailFieldValidator = new EmailFieldValidator(mEmailLayout);
        mEmailLayout.setOnClickListener(this);
        mEmailEditText.setOnClickListener(this);

        TextView headerText = view.findViewById(R.id.header_text);
        if (headerText != null) {
            headerText.setVisibility(View.GONE);
        }

        ImeHelper.setImeOnDoneListener(mEmailEditText, this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mEmailEditText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        // Set listeners for our new sign‑in and sign‑up buttons.
        mSignInButton.setOnClickListener(this);
        mSignUpButton.setOnClickListener(this);

        TextView termsText = view.findViewById(R.id.email_tos_and_pp_text);
        TextView footerText = view.findViewById(R.id.email_footer_tos_and_pp_text);
        FlowParameters flowParameters = getFlowParams();

        if (!flowParameters.shouldShowProviderChoice()) {
            PrivacyDisclosureUtils.setupTermsOfServiceAndPrivacyPolicyText(requireContext(),
                    flowParameters,
                    termsText);
        } else {
            termsText.setVisibility(View.GONE);
            PrivacyDisclosureUtils.setupTermsOfServiceFooter(requireContext(),
                    flowParameters,
                    footerText);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mHandler = new ViewModelProvider(this).get(CheckEmailHandler.class);
        mHandler.init(getFlowParams());

        FragmentActivity activity = getActivity();
        if (!(activity instanceof CheckEmailListener)) {
            throw new IllegalStateException("Activity must implement CheckEmailListener");
        }
        mListener = (CheckEmailListener) activity;

        // Removed the observer on mHandler.getOperation() since we no longer rely on provider info.

        if (savedInstanceState == null) {
            String email = getArguments().getString(ExtraConstants.EMAIL);
            if (!TextUtils.isEmpty(email)) {
                mEmailEditText.setText(email);
                // Previously auto-triggering the check is now removed.
            } else if (getFlowParams().enableCredentials) {
                mHandler.fetchCredential();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mHandler.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.button_sign_in) {
            signIn();
        } else if (id == R.id.button_sign_up) {
            signUp();
        } else if (id == R.id.email_layout || id == R.id.email) {
            mEmailLayout.setError(null);
        }
    }

    @Override
    public void onDonePressed() {
        // When the user hits “done” on the keyboard, default to sign‑in.
        signIn();
    }

    private String getEmailProvider() {
        // Iterate through all IdpConfig entries
        for (AuthUI.IdpConfig config : getFlowParams().providers) {
            // Assuming there is a getter for the provider ID
            if (EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD.equals(config.getProviderId())) {
                return EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD;
            }
        }
        // Default to standard email/password
        return EmailAuthProvider.PROVIDER_ID;
    }

    private void signIn() {
        String email = mEmailEditText.getText().toString();
        if (mEmailFieldValidator.validate(email)) {
            String provider = getEmailProvider();
            User user = new User.Builder(provider, email).build();
            mListener.onExistingEmailUser(user);
        }
    }

    private void signUp() {
        String email = mEmailEditText.getText().toString();
        if (mEmailFieldValidator.validate(email)) {
            String provider = getEmailProvider();
            User user = new User.Builder(provider, email).build();
            mListener.onNewUser(user);
        }
    }

    @Override
    public void showProgress(int message) {
        // Disable both buttons while progress is showing.
        mSignInButton.setEnabled(false);
        mSignUpButton.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        mSignInButton.setEnabled(true);
        mSignUpButton.setEnabled(true);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    /**
     * Interface to be implemented by Activities hosting this Fragment.
     */
    interface CheckEmailListener {

        /**
         * Email entered belongs to an existing email user (sign‑in flow).
         */
        void onExistingEmailUser(User user);

        /**
         * Email entered belongs to an existing IDP user.
         */
        void onExistingIdpUser(User user);

        /**
         * Email entered does not belong to an existing user (sign‑up flow).
         */
        void onNewUser(User user);

        /**
         * Email entered corresponds to an existing user whose sign in methods we do not support.
         */
        void onDeveloperFailure(Exception e);
    }
}
