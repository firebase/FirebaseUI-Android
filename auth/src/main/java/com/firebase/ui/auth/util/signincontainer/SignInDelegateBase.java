package com.firebase.ui.auth.util.signincontainer;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ResultCodes;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.util.GoogleApiHelper;
import com.firebase.ui.auth.util.GoogleSignInHelper;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO javadoc
 */
public abstract class SignInDelegateBase extends SmartLockBase<CredentialRequestResult> {

    private static final String TAG = "SignInDelegate";
    private static final int RC_CREDENTIALS_READ = 2;
    protected static final int RC_IDP_SIGNIN = 3;
    protected final int RC_AUTH_METHOD_PICKER = 4;
    protected static final int RC_EMAIL_FLOW = 5;
    protected boolean mSmartLockEnabledForDelegate = true;

    protected Credential mCredential;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        if (savedInstance != null) {
            // We already have a running instance of this fragment
            return;
        }

        FlowParameters flowParams = mHelper.getFlowParams();
        if (flowParams.isReauth) {
            // if it's a reauth and it's not an email account, skip Smart Lock.
            List<String> providers = mHelper.getCurrentUser().getProviders();
            if (providers == null || providers.size() > 0) {
                // this is a non-email user, skip Smart Lock
                startAuthMethodChoice();
                return;
            }
        }
        if (flowParams.smartLockEnabled && mSmartLockEnabledForDelegate) {
            mHelper.showLoadingDialog(R.string.progress_dialog_loading);

            mGoogleApiClient = new GoogleApiClient.Builder(getContext().getApplicationContext())
                    .addConnectionCallbacks(this)
                    .addApi(Auth.CREDENTIALS_API)
                    .enableAutoManage(getActivity(), GoogleApiHelper.getSafeAutoManageId(), this)
                    .build();
            mGoogleApiClient.connect();

            mHelper.getCredentialsApi()
                    .request(mGoogleApiClient,
                             new CredentialRequest.Builder()
                                     .setPasswordLoginSupported(true)
                                     .setAccountTypes(getSupportedAccountTypes().toArray(new String[0]))
                                     .build())
                    .setResultCallback(this);
        } else {
            startAuthMethodChoice();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // It doesn't matter what we put here, we just don't want outState to be empty
        outState.putBoolean(ExtraConstants.HAS_EXISTING_INSTANCE, true);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResult(@NonNull CredentialRequestResult result) {
        Status status = result.getStatus();

        if (status.isSuccess()) {
            // Auto sign-in success
            handleCredential(result.getCredential());
            return;
        } else {
            if (status.hasResolution()) {
                try {
                    if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
                        mHelper.startIntentSenderForResult(
                                status.getResolution().getIntentSender(),
                                RC_CREDENTIALS_READ);
                        return;
                    } else if (!getSupportedAccountTypes().isEmpty()) {
                        mHelper.startIntentSenderForResult(
                                status.getResolution().getIntentSender(),
                                RC_CREDENTIALS_READ);
                        return;
                    }
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Failed to send Credentials intent.", e);

                }
            } else {
                Log.e(TAG, "Status message:\n" + status.getStatusMessage());
            }
        }
        startAuthMethodChoice();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // We only care about onResult
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_CREDENTIALS_READ:
                if (resultCode == ResultCodes.OK) {
                    // credential selected from SmartLock, log in with that credential
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    handleCredential(credential);
                } else {
                    // Smart lock selector cancelled, go to the AuthMethodPicker screen
                    startAuthMethodChoice();
                }
                break;
            case RC_IDP_SIGNIN:
            case RC_AUTH_METHOD_PICKER:
            case RC_EMAIL_FLOW:
                finish(resultCode, data);
                break;
            default:
                IdpSignInContainer signInContainer = IdpSignInContainer.getInstance(getActivity());
                if (signInContainer != null) {
                    signInContainer.onActivityResult(requestCode, resultCode, data);
                }
        }
    }

    private List<String> getSupportedAccountTypes() {
        List<String> accounts = new ArrayList<>();
        for (AuthUI.IdpConfig idpConfig : mHelper.getFlowParams().providerInfo) {
            String providerId = idpConfig.getProviderId();
            if (providerId.equals(GoogleAuthProvider.PROVIDER_ID)
                    || providerId.equals(FacebookAuthProvider.PROVIDER_ID)
                    || providerId.equals(TwitterAuthProvider.PROVIDER_ID)) {
                accounts.add(providerIdToAccountType(providerId));
            }
        }
        return accounts;
    }

    protected static String getEmailFromCredential(Credential credential) {
        if (credential == null) {
            return null;
        }
        return credential.getId();
    }

    protected static String getAccountTypeFromCredential(Credential credential) {
        if (credential == null) {
            return null;
        }
        return credential.getAccountType();
    }

    protected static String getPasswordFromCredential(Credential credential) {
        if (credential == null) {
            return null;
        }
        return credential.getPassword();
    }

    protected void handleCredential(Credential credential) {
        mCredential = credential;
        String email = getEmailFromCredential(credential);
        String password = getPasswordFromCredential(credential);
        if (!TextUtils.isEmpty(email)) {
            if (TextUtils.isEmpty(password)) {
                // log in with id/provider
                redirectToIdpSignIn(email, getAccountTypeFromCredential(credential));
            } else {
                // Sign in with the email/password retrieved from SmartLock
                signInWithEmailAndPassword(email, password);
            }
        }
    }

    protected abstract void startAuthMethodChoice();

    /**
     * Begin sign in process with email and password from a SmartLock credential. On success, finish
     * with {@link ResultCodes#OK RESULT_OK}. On failure, delete the credential from SmartLock (if
     * applicable) and then launch the auth method picker flow.
     */
    protected void signInWithEmailAndPassword(final String email, String password) {
        mHelper.getFirebaseAuth()
                .signInWithEmailAndPassword(email, password)
                .addOnFailureListener(new TaskFailureLogger(
                        TAG, "Error signing in with email and password"))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        finish(ResultCodes.OK,
                               IdpResponse.getIntent(new IdpResponse(EmailAuthProvider.PROVIDER_ID,
                                                                     email)));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof FirebaseAuthInvalidUserException) {
                            // In this case the credential saved in SmartLock was not
                            // a valid credential, we should delete it from SmartLock
                            // before continuing.
                            deleteCredentialAndRedirect();
                        } else {
                            startAuthMethodChoice();
                        }
                    }
                });
    }

    /**
     * Delete the last credential retrieved from SmartLock and then redirect to the auth method
     * choice flow.
     */
    protected void deleteCredentialAndRedirect() {
        if (mCredential == null) {
            Log.w(TAG, "deleteCredentialAndRedirect: null credential");
            startAuthMethodChoice();
            return;
        }

        GoogleSignInHelper.getInstance(getActivity())
                .delete(mCredential)
                .addOnCompleteListener(new OnCompleteListener<Status>() {
                    @Override
                    public void onComplete(@NonNull Task<Status> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "deleteCredential:failure", task.getException());
                        }
                        startAuthMethodChoice();
                    }
                });
    }

    protected abstract void redirectToIdpSignIn(String email, String accountType);
}
