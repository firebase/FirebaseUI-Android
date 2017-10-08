package com.firebase.ui.auth.ui.email;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.net.Uri;
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

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.util.ui.fieldvalidators.EmailFieldValidator;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;

/**
 * Fragment that shows a form with an email field and checks for existing accounts with that email.
 * <p>
 * Host Activities should implement {@link CheckEmailListener}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CheckEmailFragment extends FragmentBase implements
        View.OnClickListener,
        ImeHelper.DonePressedListener {

    /**
     * Interface to be implemented by Activities hosting this Fragment.
     */
    interface CheckEmailListener {

        /**
         * Email entered belongs to an existing email user.
         */
        void onExistingEmailUser(User user);

        /**
         * Email entered belongs to an existing IDP user.
         */
        void onExistingIdpUser(User user);

        /**
         * Email entered does not belong to an existing user.
         */
        void onNewUser(User user);

    }

    public static final String TAG = "CheckEmailFragment";

    private CheckEmailHandler mHandler;

    private EditText mEmailEditText;
    private TextInputLayout mEmailLayout;

    private EmailFieldValidator mEmailFieldValidator;
    private CheckEmailListener mListener;

    public static CheckEmailFragment newInstance(@Nullable String email) {
        CheckEmailFragment fragment = new CheckEmailFragment();
        Bundle args = new Bundle();
        args.putString(ExtraConstants.EXTRA_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("NewApi") // TODO remove once lint understands Build.VERSION_CODES.O
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fui_check_email_layout, container, false);

        // Email field and validator
        mEmailLayout = v.findViewById(R.id.email_layout);
        mEmailEditText = v.findViewById(R.id.email);
        mEmailFieldValidator = new EmailFieldValidator(mEmailLayout);
        mEmailLayout.setOnClickListener(this);
        mEmailEditText.setOnClickListener(this);

        ImeHelper.setImeOnDoneListener(mEmailEditText, this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && getFlowHolder().getParams().enableHints) {
            mEmailEditText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        // "Next" button
        v.findViewById(R.id.button_next).setOnClickListener(this);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mHandler = ViewModelProviders.of(this).get(CheckEmailHandler.class);

        // Set listener
        if (!(getActivity() instanceof CheckEmailListener)) {
            throw new IllegalStateException("Activity must implement CheckEmailListener");
        }
        mListener = (CheckEmailListener) getActivity();

        if (savedInstanceState != null) {
            return;
        }

        // Check for email
        String email = getArguments().getString(ExtraConstants.EXTRA_EMAIL);
        if (!TextUtils.isEmpty(email)) {
            // Use email passed in
            mEmailEditText.setText(email);
            validateAndProceed();
        } else if (getFlowHolder().getParams().enableHints) {
            // Try SmartLock email autocomplete hint
            mHandler.fetchCredential().observe(this, new Observer<Credential>() {
                @Override
                public void onChanged(@Nullable Credential credential) {
                    if (credential != null) {
                        mEmailEditText.setText(credential.getId());
                        validateAndProceed();
                    }
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ExtraConstants.HAS_EXISTING_INSTANCE, true);
        super.onSaveInstanceState(outState);
    }

    private void validateAndProceed() {
        String email = mEmailEditText.getText().toString();
        if (mEmailFieldValidator.validate(email)) {
            checkAccountExists(email);
        }
    }

    private void checkAccountExists(@NonNull final String email) {
        getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_checking_accounts);

        // Get name from SmartLock, if possible
        @Nullable Credential credential = mHandler.getCredentialListener().getValue();
        String name = null;
        Uri photoUri = null;
        if (credential != null && credential.getId().equals(email)) {
            name = credential.getName();
            photoUri = credential.getProfilePictureUri();
        }

        final String finalName = name;
        final Uri finalPhotoUri = photoUri;

        mHandler.getTopProvider(email)
                .addOnSuccessListener(getActivity(), new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String provider) {
                        if (provider == null) {
                            mListener.onNewUser(new User.Builder(EmailAuthProvider.PROVIDER_ID,
                                    email)
                                    .setName(finalName)
                                    .setPhotoUri(finalPhotoUri)
                                    .build());
                        } else if (EmailAuthProvider.PROVIDER_ID.equalsIgnoreCase(provider)) {
                            mListener.onExistingEmailUser(
                                    new User.Builder(EmailAuthProvider.PROVIDER_ID, email).build());
                        } else {
                            mListener.onExistingIdpUser(new User.Builder(provider, email).build());
                        }
                    }
                })
                .addOnCompleteListener(
                        getActivity(),
                        new OnCompleteListener<String>() {
                            @Override
                            public void onComplete(@NonNull Task<String> task) {
                                getDialogHolder().dismissDialog();
                            }
                        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.button_next) {
            validateAndProceed();
        } else if (id == R.id.email_layout || id == R.id.email) {
            mEmailLayout.setError(null);
        }
    }

    @Override
    public void onDonePressed() {
        validateAndProceed();
    }
}
