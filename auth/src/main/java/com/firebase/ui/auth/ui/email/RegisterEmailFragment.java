package com.firebase.ui.auth.ui.email;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.util.ui.fieldvalidators.BaseValidator;
import com.firebase.ui.auth.util.ui.fieldvalidators.EmailFieldValidator;
import com.firebase.ui.auth.util.ui.fieldvalidators.NoOpValidator;
import com.firebase.ui.auth.util.ui.fieldvalidators.PasswordFieldValidator;
import com.firebase.ui.auth.util.ui.fieldvalidators.RequiredFieldValidator;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.firebase.ui.auth.viewmodel.email.EmailProviderResponseHandler;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/**
 * Fragment to display an email/name/password sign up form for new users.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RegisterEmailFragment extends FragmentBase implements
        View.OnClickListener, View.OnFocusChangeListener, ImeHelper.DonePressedListener {
    public static final String TAG = "RegisterEmailFragment";

    private EmailProviderResponseHandler mHandler;

    private Button mNextButton;
    private ProgressBar mProgressBar;

    private EditText mEmailEditText;
    private EditText mNameEditText;
    private EditText mPasswordEditText;
    private TextInputLayout mEmailInput;
    private TextInputLayout mPasswordInput;

    private EmailFieldValidator mEmailFieldValidator;
    private PasswordFieldValidator mPasswordFieldValidator;
    private BaseValidator mNameValidator;

    private AnonymousUpgradeListener mListener;
    private User mUser;

    /**
     * Interface to be implemented by Activities hosting this Fragment.
     */
    interface AnonymousUpgradeListener {

        /**
         * Email belongs to an existing user - failed to merge anonymous user.
         */
        void onMergeFailure(IdpResponse response);

    }

    public static RegisterEmailFragment newInstance(User user) {
        RegisterEmailFragment fragment = new RegisterEmailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ExtraConstants.USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mUser = User.getUser(getArguments());
        } else {
            mUser = User.getUser(savedInstanceState);
        }

        mHandler = ViewModelProviders.of(this).get(EmailProviderResponseHandler.class);
        mHandler.init(getFlowParams());
        mHandler.getOperation().observe(this, new ResourceObserver<IdpResponse>(
                this, R.string.fui_progress_dialog_signing_up) {
            @Override
            protected void onSuccess(@NonNull IdpResponse response) {
                startSaveCredentials(
                        mHandler.getCurrentUser(),
                        response,
                        mPasswordEditText.getText().toString());
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                if (e instanceof FirebaseAuthWeakPasswordException) {
                    mPasswordInput.setError(getResources().getQuantityString(
                            R.plurals.fui_error_weak_password,
                            R.integer.fui_min_password_length));
                } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    mEmailInput.setError(getString(R.string.fui_invalid_email_address));
                } else if (e instanceof FirebaseAuthAnonymousUpgradeException) {
                    IdpResponse response = ((FirebaseAuthAnonymousUpgradeException) e).getResponse();
                    mListener.onMergeFailure(response);
                } else {
                    // General error message, this branch should not be invoked but
                    // covers future API changes
                    mEmailInput.setError(getString(R.string.fui_email_account_creation_error));
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fui_register_email_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mNextButton = view.findViewById(R.id.button_create);
        mProgressBar = view.findViewById(R.id.top_progress_bar);

        mEmailEditText = view.findViewById(R.id.email);
        mNameEditText = view.findViewById(R.id.name);
        mPasswordEditText = view.findViewById(R.id.password);
        mEmailInput = view.findViewById(R.id.email_layout);
        mPasswordInput = view.findViewById(R.id.password_layout);
        TextInputLayout nameInput = view.findViewById(R.id.name_layout);

        // Get configuration
        AuthUI.IdpConfig emailConfig = ProviderUtils.getConfigFromIdpsOrThrow(
                getFlowParams().providers, EmailAuthProvider.PROVIDER_ID);
        boolean requireName = emailConfig.getParams()
                .getBoolean(ExtraConstants.REQUIRE_NAME, true);
        mPasswordFieldValidator = new PasswordFieldValidator(
                mPasswordInput,
                getResources().getInteger(R.integer.fui_min_password_length));
        mNameValidator = requireName
                ? new RequiredFieldValidator(nameInput)
                : new NoOpValidator(nameInput);
        mEmailFieldValidator = new EmailFieldValidator(mEmailInput);

        ImeHelper.setImeOnDoneListener(mPasswordEditText, this);

        mEmailEditText.setOnFocusChangeListener(this);
        mNameEditText.setOnFocusChangeListener(this);
        mPasswordEditText.setOnFocusChangeListener(this);
        mNextButton.setOnClickListener(this);

        // Only show the name field if required
        nameInput.setVisibility(requireName ? View.VISIBLE : View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getFlowParams().enableCredentials) {
            mEmailEditText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        TextView footerText = view.findViewById(R.id.email_footer_tos_and_pp_text);
        PrivacyDisclosureUtils.setupTermsOfServiceFooter(
                requireContext(), getFlowParams(), footerText);

        // WARNING: Nothing below this line will be executed on rotation
        if (savedInstanceState != null) {
            return;
        }

        // If email is passed in, fill in the field and move down to the name field.
        String email = mUser.getEmail();
        if (!TextUtils.isEmpty(email)) {
            mEmailEditText.setText(email);
        }

        // If name is passed in, fill in the field and move down to the password field.
        String name = mUser.getName();
        if (!TextUtils.isEmpty(name)) {
            mNameEditText.setText(name);
        }

        // See http://stackoverflow.com/questions/11082341/android-requestfocus-ineffective#comment51774752_11082523
        if (!requireName || !TextUtils.isEmpty(mNameEditText.getText())) {
            safeRequestFocus(mPasswordEditText);
        } else if (!TextUtils.isEmpty(mEmailEditText.getText())) {
            safeRequestFocus(mNameEditText);
        } else {
            safeRequestFocus(mEmailEditText);
        }
    }

    private void safeRequestFocus(final View v) {
        v.post(new Runnable() {
            @Override
            public void run() {
                v.requestFocus();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentActivity activity = requireActivity();
        activity.setTitle(R.string.fui_title_register_email);
        if (!(activity instanceof AnonymousUpgradeListener)) {
            throw new IllegalStateException("Activity must implement CheckEmailListener");
        }
        mListener = (AnonymousUpgradeListener) activity;

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(ExtraConstants.USER,
                new User.Builder(EmailAuthProvider.PROVIDER_ID, mEmailEditText.getText().toString())
                        .setName(mNameEditText.getText().toString())
                        .setPhotoUri(mUser.getPhotoUri())
                        .build());
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (hasFocus) return; // Only consider fields losing focus

        int id = view.getId();
        if (id == R.id.email) {
            mEmailFieldValidator.validate(mEmailEditText.getText());
        } else if (id == R.id.name) {
            mNameValidator.validate(mNameEditText.getText());
        } else if (id == R.id.password) {
            mPasswordFieldValidator.validate(mPasswordEditText.getText());
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_create) {
            validateAndRegisterUser();
        }
    }

    @Override
    public void onDonePressed() {
        validateAndRegisterUser();
    }

    @Override
    public void showProgress(int message) {
        mNextButton.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        mNextButton.setEnabled(true);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    private void validateAndRegisterUser() {
        String email = mEmailEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        String name = mNameEditText.getText().toString();

        boolean emailValid = mEmailFieldValidator.validate(email);
        boolean passwordValid = mPasswordFieldValidator.validate(password);
        boolean nameValid = mNameValidator.validate(name);
        if (emailValid && passwordValid && nameValid) {
            mHandler.startSignIn(new IdpResponse.Builder(
                            new User.Builder(EmailAuthProvider.PROVIDER_ID, email)
                                    .setName(name)
                                    .setPhotoUri(mUser.getPhotoUri())
                                    .build())
                            .build(),
                    password);
        }
    }
}
