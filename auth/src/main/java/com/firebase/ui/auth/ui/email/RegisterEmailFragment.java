package com.firebase.ui.auth.ui.email;

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
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.ProfileMerger;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.idp.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.util.ui.PreambleHandler;
import com.firebase.ui.auth.util.ui.fieldvalidators.BaseValidator;
import com.firebase.ui.auth.util.ui.fieldvalidators.EmailFieldValidator;
import com.firebase.ui.auth.util.ui.fieldvalidators.NoOpValidator;
import com.firebase.ui.auth.util.ui.fieldvalidators.PasswordFieldValidator;
import com.firebase.ui.auth.util.ui.fieldvalidators.RequiredFieldValidator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/**
 * Fragment to display an email/name/password sign up form for new users.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RegisterEmailFragment extends FragmentBase implements
        View.OnClickListener, View.OnFocusChangeListener, ImeHelper.DonePressedListener {

    public static final String TAG = "RegisterEmailFragment";

    private RegistrationListener mListener;

    private EditText mEmailEditText;
    private EditText mNameEditText;
    private EditText mPasswordEditText;
    private TextView mAgreementText;
    private TextInputLayout mNameInput;
    private TextInputLayout mEmailInput;
    private TextInputLayout mPasswordInput;

    private EmailFieldValidator mEmailFieldValidator;
    private PasswordFieldValidator mPasswordFieldValidator;
    private BaseValidator mNameValidator;

    private User mUser;

    public interface RegistrationListener {

        void onRegistrationSuccess(AuthResult authResult, String password, IdpResponse response);

    }

    public static RegisterEmailFragment newInstance(FlowParameters flowParameters, User user) {
        RegisterEmailFragment fragment = new RegisterEmailFragment();

        Bundle args = new Bundle();
        args.putParcelable(ExtraConstants.EXTRA_FLOW_PARAMS, flowParameters);
        args.putParcelable(ExtraConstants.EXTRA_USER, user);

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
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fui_register_email_layout, container, false);

        // Get configuration
        AuthUI.IdpConfig emailConfig = ProviderUtils.getConfigFromIdps(
                getFlowParams().providerInfo, EmailAuthProvider.PROVIDER_ID);
        boolean requireName = emailConfig.getParams()
                .getBoolean(ExtraConstants.EXTRA_REQUIRE_NAME, true);

        mEmailEditText = v.findViewById(R.id.email);
        mNameEditText = v.findViewById(R.id.name);
        mPasswordEditText = v.findViewById(R.id.password);
        mAgreementText = v.findViewById(R.id.create_account_text);
        mEmailInput = v.findViewById(R.id.email_layout);
        mNameInput = v.findViewById(R.id.name_layout);
        mPasswordInput = v.findViewById(R.id.password_layout);

        mPasswordFieldValidator = new PasswordFieldValidator(
                mPasswordInput,
                getResources().getInteger(R.integer.fui_min_password_length));
        mNameValidator = requireName
                ? new RequiredFieldValidator(mNameInput)
                : new NoOpValidator(mNameInput);
        mEmailFieldValidator = new EmailFieldValidator(mEmailInput);

        ImeHelper.setImeOnDoneListener(mPasswordEditText, this);

        mEmailEditText.setOnFocusChangeListener(this);
        mNameEditText.setOnFocusChangeListener(this);
        mPasswordEditText.setOnFocusChangeListener(this);
        v.findViewById(R.id.button_create).setOnClickListener(this);

        // Only show the name field if required
        if (requireName) {
            mNameInput.setVisibility(View.VISIBLE);
        } else {
            mNameInput.setVisibility(View.GONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getFlowParams().enableCredentials) {
            mEmailEditText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        if (savedInstanceState != null) {
            return v;
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

        return v;

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
        getActivity().setTitle(R.string.fui_title_register_email);

        if (!(getActivity() instanceof RegistrationListener)) {
            throw new RuntimeException("Must be attached to a RegistrationListener.");
        }

        mListener = (RegistrationListener) getActivity();
        PreambleHandler.setup(getContext(),
                getFlowParams(),
                R.string.fui_button_text_save,
                mAgreementText);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(ExtraConstants.EXTRA_USER,
                new User.Builder(EmailAuthProvider.PROVIDER_ID, mEmailEditText.getText().toString())
                        .setName(mNameEditText.getText().toString())
                        .setPhotoUri(mUser.getPhotoUri())
                        .build());
        super.onSaveInstanceState(outState);
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

    private void validateAndRegisterUser() {
        String email = mEmailEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        String name = mNameEditText.getText().toString();

        boolean emailValid = mEmailFieldValidator.validate(email);
        boolean passwordValid = mPasswordFieldValidator.validate(password);
        boolean nameValid = mNameValidator.validate(name);
        if (emailValid && passwordValid && nameValid) {
            getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_signing_up);
            registerUser(email, name, password);
        }
    }

    private void registerUser(final String email, final String name, final String password) {
        final IdpResponse response = new IdpResponse.Builder(
                new User.Builder(EmailAuthProvider.PROVIDER_ID, email)
                        .setName(name)
                        .setPhotoUri(mUser.getPhotoUri())
                        .build())
                .build();

        getAuthHelper().getFirebaseAuth()
                .createUserWithEmailAndPassword(email, password)
                .continueWithTask(new ProfileMerger(response))
                .addOnFailureListener(new TaskFailureLogger(TAG, "Error creating user"))
                .addOnSuccessListener(getActivity(), new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        mListener.onRegistrationSuccess(authResult, password, response);
                    }
                })
                .addOnFailureListener(getActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof FirebaseAuthWeakPasswordException) {
                            // Password too weak
                            mPasswordInput.setError(getResources().getQuantityString(
                                    R.plurals.fui_error_weak_password, R.integer.fui_min_password_length));
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            // Email address is malformed
                            mEmailInput.setError(getString(R.string.fui_invalid_email_address));
                        } else if (e instanceof FirebaseAuthUserCollisionException) {
                            // Collision with existing user email, it should be very hard for
                            // the user to even get to this error due to CheckEmailFragment.

                            FirebaseAuth auth = getAuthHelper().getFirebaseAuth();
                            ProviderUtils.fetchTopProvider(auth, email).addOnSuccessListener(
                                    getActivity(),
                                    new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String provider) {
                                            Toast.makeText(getContext(),
                                                    R.string.fui_error_user_collision,
                                                    Toast.LENGTH_LONG)
                                                    .show();

                                            if (provider == null) {
                                                throw new IllegalStateException(
                                                        "User has no providers even though " +
                                                                "we got a " +
                                                                "FirebaseAuthUserCollisionException");
                                            } else if (EmailAuthProvider.PROVIDER_ID.equalsIgnoreCase(
                                                    provider)) {
                                                getActivity().startActivityForResult(
                                                        WelcomeBackPasswordPrompt.createIntent(
                                                                getContext(),
                                                                getFlowParams(),
                                                                new IdpResponse.Builder(new User.Builder(
                                                                        EmailAuthProvider.PROVIDER_ID,
                                                                        email).build()).build()),
                                                        EmailActivity.RC_WELCOME_BACK_IDP);
                                            } else {
                                                getActivity().startActivityForResult(
                                                        WelcomeBackIdpPrompt.createIntent(
                                                                getContext(),
                                                                getFlowParams(),
                                                                new User.Builder(provider, email)
                                                                        .build(),
                                                                null),
                                                        EmailActivity.RC_WELCOME_BACK_IDP);
                                            }
                                        }
                                    })
                                    .addOnCompleteListener(new OnCompleteListener<String>() {
                                        @Override
                                        public void onComplete(@NonNull Task<String> task) {
                                            getDialogHolder().dismissDialog();
                                        }
                                    });
                            return;
                        } else {
                            // General error message, this branch should not be invoked but
                            // covers future API changes
                            mEmailInput.setError(getString(R.string.fui_email_account_creation_error));
                        }

                        getDialogHolder().dismissDialog();
                    }
                });
    }
}
