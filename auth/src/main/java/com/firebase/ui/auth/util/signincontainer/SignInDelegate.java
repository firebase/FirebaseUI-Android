package com.firebase.ui.auth.util.signincontainer;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ResultCodes;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.FragmentHelper;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.User;
import com.firebase.ui.auth.ui.email.RegisterEmailActivity;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.util.GoogleApiHelper;
import com.firebase.ui.auth.util.GoogleSignInHelper;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
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
 * Attempts to acquire a credential from Smart Lock for Passwords to sign in
 * an existing account. If this succeeds, an attempt is made to sign the user in
 * with this credential. If it does not, the
 * {@link AuthMethodPickerActivity authentication method picker activity}
 * is started, unless only email is supported, in which case the
 * {@link RegisterEmailActivity} is started.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SignInDelegate extends SmartLockBase<CredentialRequestResult> {
    private static final String TAG = "SignInDelegate";
    private static final int RC_CREDENTIALS_READ = 2;
    private static final int RC_IDP_SIGNIN = 3;
    private static final int RC_AUTH_METHOD_PICKER = 4;
    private static final int RC_EMAIL_FLOW = 5;

    private Credential mCredential;

    public static void delegate(FragmentActivity activity, FlowParameters params) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (!(fragment instanceof SignInDelegate)) {
            SignInDelegate result = new SignInDelegate();
            result.setArguments(FragmentHelper.getFlowParamsBundle(params));
            fm.beginTransaction().add(result, TAG).disallowAddToBackStack().commit();
        }
    }

    public static SignInDelegate getInstance(FragmentActivity activity) {
        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TAG);
        if (fragment instanceof SignInDelegate) {
            return (SignInDelegate) fragment;
        } else {
            return null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        if (savedInstance != null) {
            // We already have a running instance of this fragment
            return;
        }

        if (mHelper.getFlowParams().smartLockEnabled) {
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
        } else if (status.hasResolution()) {
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

    private String getEmailFromCredential() {
        if (mCredential == null) {
            return null;
        }
        return mCredential.getId();
    }

    private String getAccountTypeFromCredential() {
        if (mCredential == null) {
            return null;
        }
        return mCredential.getAccountType();
    }

    private String getPasswordFromCredential() {
        if (mCredential == null) {
            return null;
        }
        return mCredential.getPassword();
    }

    private void handleCredential(Credential credential) {
        mCredential = credential;
        String email = getEmailFromCredential();
        String password = getPasswordFromCredential();
        if (!TextUtils.isEmpty(email)) {
            if (TextUtils.isEmpty(password)) {
                // log in with id/provider
                redirectToIdpSignIn(email, getAccountTypeFromCredential());
            } else {
                // Sign in with the email/password retrieved from SmartLock
                signInWithEmailAndPassword(email, password);
            }
        }
    }

    private void startAuthMethodChoice() {
        List<AuthUI.IdpConfig> idpConfigs = mHelper.getFlowParams().providerInfo;

        // If the only provider is Email, immediately launch the email flow. Otherwise, launch
        // the auth method picker screen.
        if (idpConfigs.size() == 1) {
            if (idpConfigs.get(0).getProviderId().equals(EmailAuthProvider.PROVIDER_ID)) {
                startActivityForResult(
                        RegisterEmailActivity.createIntent(getContext(), mHelper.getFlowParams()),
                        RC_EMAIL_FLOW);
            } else {
                redirectToIdpSignIn(null,
                                    providerIdToAccountType(idpConfigs.get(0).getProviderId()));
            }
        } else {
            startActivityForResult(
                    AuthMethodPickerActivity.createIntent(
                            getContext(),
                            mHelper.getFlowParams()),
                    RC_AUTH_METHOD_PICKER);
        }
        mHelper.dismissDialog();
    }

    /**
     * Begin sign in process with email and password from a SmartLock credential.
     * On success, finish with {@link ResultCodes#OK RESULT_OK}.
     * On failure, delete the credential from SmartLock (if applicable) and then launch the
     * auth method picker flow.
     */
    private void signInWithEmailAndPassword(final String email, String password) {
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
     * Delete the last credential retrieved from SmartLock and then redirect to the
     * auth method choice flow.
     */
    private void deleteCredentialAndRedirect() {
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

    private void redirectToIdpSignIn(String email, String accountType) {
        if (TextUtils.isEmpty(accountType)) {
            startActivityForResult(
                    RegisterEmailActivity.createIntent(
                            getContext(),
                            mHelper.getFlowParams(),
                            email),
                    RC_EMAIL_FLOW);
            return;
        }

        if (accountType.equals(IdentityProviders.GOOGLE)
                || accountType.equals(IdentityProviders.FACEBOOK)
                || accountType.equals(IdentityProviders.TWITTER)) {
            IdpSignInContainer.signIn(
                    getActivity(),
                    mHelper.getFlowParams(),
                    new User.Builder(email)
                            .setProvider(accountTypeToProviderId(accountType))
                            .build());
        } else {
            Log.w(TAG, "unknown provider: " + accountType);
            startActivityForResult(
                    AuthMethodPickerActivity.createIntent(
                            getContext(),
                            mHelper.getFlowParams()),
                    RC_IDP_SIGNIN);
            mHelper.dismissDialog();
        }
    }
}
