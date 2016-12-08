package com.firebase.ui.auth.ui.email;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.BaseFragment;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.fieldvalidators.EmailFieldValidator;
import com.firebase.ui.auth.util.FirebaseAuthWrapperFactory;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;

import java.util.List;

/**
 * Fragment that shows a form with an email field and checks for existing accounts with that
 * email.
 *
 * Host Activities should implement {@link CheckEmailListener}.
 */
public class CheckEmailFragment extends BaseFragment implements
        View.OnClickListener {

    /**
     * Interface to be implemented by Activities hosting this Fragment.
     */
     interface CheckEmailListener {

        /**
         * Email entered belongs to an existing email user.
         */
        void onExistingEmailUser(@NonNull String email);

        /**
         * Email entered belongs to an existing IDP user.
         */
        void onExistingIdpUser(@NonNull String email, @NonNull String provider);

        /**
         * Email entered does not beling to an existing user.
         */
        void onNewUser(@NonNull String email, @Nullable String name);

    }

    public static final String TAG = "CheckEmailFragment";

    private static final int RC_HINT = 13;
    private static final int RC_WELCOME_BACK_IDP = 15;
    private static final int RC_SIGN_IN = 16;

    private EditText mEmailEditText;

    private EmailFieldValidator mEmailFieldValidator;
    private CheckEmailListener mListener;

    private Credential mLastCredential;

    public static CheckEmailFragment getInstance(@NonNull FlowParameters flowParameters,
                                                 @Nullable String email) {
        CheckEmailFragment fragment = new CheckEmailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ExtraConstants.EXTRA_FLOW_PARAMS, flowParameters);
        args.putString(ExtraConstants.EXTRA_EMAIL, email);

        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.check_email_layout, container, false);

        // Email field and validator
        mEmailEditText = (EditText) v.findViewById(R.id.email);
        mEmailFieldValidator = new EmailFieldValidator(
                (TextInputLayout) v.findViewById(R.id.email_layout));

        // "Next" button
        v.findViewById(R.id.button_next).setOnClickListener(this);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set title
        if (getActivity().getActionBar() != null) {
            getActivity().getActionBar().setTitle(R.string.title_check_email);
        }

        // Set listener
        if (!(getActivity() instanceof CheckEmailListener)) {
            throw new IllegalStateException("Activity must implement CheckEmailListener");
        }
        this.mListener = (CheckEmailListener) getActivity();

        // Check for email
        String email = getArguments().getString(ExtraConstants.EXTRA_EMAIL);
        if (!TextUtils.isEmpty(email)) {
            // Use email passed in
            mEmailEditText.setText(email);
            validateAndProceed();
        } else if (mHelper.getFlowParams().smartLockEnabled) {
            // Try SmartLock email autocomplete hint
            showEmailAutoCompleteHint();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_HINT:
                if (data != null) {
                    mLastCredential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    if (mLastCredential != null) {
                        // Get the email from the credential
                        mEmailEditText.setText(mLastCredential.getId());

                        // Attempt to proceed
                        validateAndProceed();
                    }
                }
                break;
            case RC_SIGN_IN:
            case RC_WELCOME_BACK_IDP:
                finish(resultCode, data);
                break;
        }
    }

    public void validateAndProceed() {
        String email = mEmailEditText.getText().toString();
        if (mEmailFieldValidator.validate(email)) {
            checkAccountExists(email);
        }
    }

    public void checkAccountExists(@NonNull final String email) {
        mHelper.showLoadingDialog(R.string.progress_dialog_checking_accounts);

        if (!TextUtils.isEmpty(email)) {
            mHelper.getFirebaseAuth()
                    .fetchProvidersForEmail(email)
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error fetching providers for email"))
                    .addOnCompleteListener(getActivity(), new OnCompleteListener<ProviderQueryResult>() {
                        @Override
                        public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                            mHelper.dismissDialog();
                        }
                    })
                    .addOnSuccessListener(getActivity(), new OnSuccessListener<ProviderQueryResult>() {
                        @Override
                        public void onSuccess(ProviderQueryResult result) {
                            List<String> providers = result.getProviders();
                            if (providers == null || providers.isEmpty()) {
                                // Get name from SmartLock, if possible
                                String name = null;
                                if (mLastCredential != null && mLastCredential.getId().equals(email)) {
                                    name = mLastCredential.getName();
                                }

                                mListener.onNewUser(email, name);
                            } else if (EmailAuthProvider.PROVIDER_ID.equalsIgnoreCase(providers.get(0))) {
                                mListener.onExistingEmailUser(email);
                            } else {
                                mListener.onExistingIdpUser(email, providers.get(0));
                            }
                        }
                    });
        }
    }

    private void showEmailAutoCompleteHint() {
        PendingIntent hintIntent = FirebaseAuthWrapperFactory
                .getFirebaseAuthWrapper(mHelper.getAppName())
                .getEmailHintIntent(getActivity());
        if (hintIntent != null) {
            try {
                startIntentSenderForResult(hintIntent.getIntentSender(), RC_HINT, null, 0, 0, 0, null);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Unable to start hint intent", e);
            }
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.button_next) {
            validateAndProceed();
        }
    }
}
