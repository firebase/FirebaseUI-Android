package com.firebase.ui.auth.ui.email;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.User;
import com.firebase.ui.auth.ui.email.fieldvalidators.EmailFieldValidator;
import com.firebase.ui.auth.util.GoogleApiConstants;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;

import java.util.List;

/**
 * Fragment that shows a form with an email field and checks for existing accounts with that
 * email.
 * <p>
 * Host Activities should implement {@link CheckEmailListener}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CheckEmailFragment extends FragmentBase implements View.OnClickListener {
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
         * Email entered does not beling to an existing user.
         */
        void onNewUser(User user);

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
        } else if (mHelper.getFlowParams().smartLockEnabled) {
            // Try SmartLock email autocomplete hint
            showEmailAutoCompleteHint();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ExtraConstants.HAS_EXISTING_INSTANCE, true);
        super.onSaveInstanceState(outState);
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
                    .addOnCompleteListener(
                            getActivity(),
                            new OnCompleteListener<ProviderQueryResult>() {
                                @Override
                                public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                                    mHelper.dismissDialog();
                                }
                            })
                    .addOnSuccessListener(
                            getActivity(),
                            new OnSuccessListener<ProviderQueryResult>() {
                                @Override
                                public void onSuccess(ProviderQueryResult result) {
                                    List<String> providers = result.getProviders();
                                    if (providers == null || providers.isEmpty()) {
                                        // Get name from SmartLock, if possible
                                        String name = null;
                                        Uri photoUri = null;
                                        if (mLastCredential != null && mLastCredential.getId().equals(email)) {
                                            name = mLastCredential.getName();
                                            photoUri = mLastCredential.getProfilePictureUri();
                                        }

                                        mListener.onNewUser(new User.Builder(email)
                                                                    .setName(name)
                                                                    .setPhotoUri(photoUri)
                                                                    .build());
                                    } else if (EmailAuthProvider.PROVIDER_ID.equalsIgnoreCase(providers.get(0))) {
                                        mListener.onExistingEmailUser(new User.Builder(email).build());
                                    } else {
                                        mListener.onExistingIdpUser(
                                                new User.Builder(email)
                                                        .setProvider(providers.get(0))
                                                        .build());
                                    }
                                }
                            });
        }
    }

    private void showEmailAutoCompleteHint() {
        try {
            mHelper.startIntentSenderForResult(getEmailHintIntent().getIntentSender(), RC_HINT);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Unable to start hint intent", e);
        }
    }

    private PendingIntent getEmailHintIntent() {
        GoogleApiClient client = new GoogleApiClient.Builder(getContext())
                .addApi(Auth.CREDENTIALS_API)
                .enableAutoManage(getActivity(), GoogleApiConstants.AUTO_MANAGE_ID3,
                                  new GoogleApiClient.OnConnectionFailedListener() {
                                      @Override
                                      public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                                          Log.e(TAG,
                                                "Client connection failed: " + connectionResult.getErrorMessage());
                                      }
                                  })
                .build();

        HintRequest hintRequest = new HintRequest.Builder()
                .setHintPickerConfig(new CredentialPickerConfig.Builder()
                                             .setShowCancelButton(true)
                                             .build())
                .setEmailAddressIdentifierSupported(true)
                .build();

        return Auth.CredentialsApi.getHintPickerIntent(client, hintRequest);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.button_next) {
            validateAndProceed();
        }
    }
}
