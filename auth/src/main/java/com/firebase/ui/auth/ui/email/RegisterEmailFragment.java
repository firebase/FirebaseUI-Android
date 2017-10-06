package com.firebase.ui.auth.ui.email;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.User;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.ui.ImeHelper;
import com.firebase.ui.auth.ui.email.fieldvalidators.EmailFieldValidator;
import com.firebase.ui.auth.ui.email.fieldvalidators.PasswordFieldValidator;
import com.firebase.ui.auth.ui.email.fieldvalidators.RequiredFieldValidator;
import com.google.android.gms.tasks.Tasks;
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

    private EditText mEmailEditText;
    private EditText mNameEditText;
    private EditText mPasswordEditText;
    private TextInputLayout mEmailInput;
    private TextInputLayout mPasswordInput;

    private EmailFieldValidator mEmailFieldValidator;
    private PasswordFieldValidator mPasswordFieldValidator;
    private RequiredFieldValidator mNameValidator;

    private User mUser;

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

        getSignInHandler().getSuccessLiveData().observe(this, new Observer<IdpResponse>() {
            @Override
            public void onChanged(@Nullable IdpResponse response) {

            }
        });
        getSignInHandler().getFailureLiveData().observe(this, new Observer<Exception>() {
            @Override
            public void onChanged(@Nullable Exception e) {
                if (e instanceof FirebaseAuthWeakPasswordException) {
                    mPasswordInput.setError(getResources().getQuantityString(
                            R.plurals.fui_error_weak_password,
                            R.integer.fui_min_password_length));
                } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    mEmailInput.setError(getString(R.string.fui_invalid_email_address));
                } else {
                    // General error message, this branch should not be invoked but
                    // covers future API changes
                    mEmailInput.setError(getString(R.string.fui_email_account_creation_error));
                }

                getDialogHolder().dismissDialog();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fui_register_email_layout, container, false);
    }

    @SuppressLint("NewApi") // TODO remove once lint understands Build.VERSION_CODES.O
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mEmailEditText = view.findViewById(R.id.email);
        mNameEditText = view.findViewById(R.id.name);
        mPasswordEditText = view.findViewById(R.id.password);
        mEmailInput = view.findViewById(R.id.email_layout);
        mPasswordInput = view.findViewById(R.id.password_layout);

        mEmailFieldValidator = new EmailFieldValidator(mEmailInput);
        mNameValidator = new RequiredFieldValidator(
                view.<TextInputLayout>findViewById(R.id.name_layout));
        mPasswordFieldValidator = new PasswordFieldValidator(
                mPasswordInput,
                getResources().getInteger(R.integer.fui_min_password_length));

        ImeHelper.setImeOnDoneListener(mPasswordEditText, this);

        mEmailEditText.setOnFocusChangeListener(this);
        mNameEditText.setOnFocusChangeListener(this);
        mPasswordEditText.setOnFocusChangeListener(this);
        view.findViewById(R.id.button_create).setOnClickListener(this);

        FlowParameters params = getFlowHolder().getParams();
        new PreambleHandler(getContext(), params, R.string.fui_button_text_save)
                .setPreamble(view.<TextView>findViewById(R.id.create_account_text));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && params.enableCredentials) {
            mEmailEditText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        if (savedInstanceState != null) { return; }

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
        if (!TextUtils.isEmpty(mNameEditText.getText())) {
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
        getActivity().setTitle(R.string.fui_title_register_email);
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
            getSignInHandler().start(Tasks.forResult(new IdpResponse.Builder(
                    new User.Builder(EmailAuthProvider.PROVIDER_ID, email)
                            .setName(name)
                            .setPhotoUri(mUser.getPhotoUri())
                            .build())
                    .build()));
        }
    }
}
