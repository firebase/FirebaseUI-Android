package com.firebase.ui.auth.util.smartlock;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FragmentHelper;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.AuthCredentialHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.SignInNoPasswordActivity;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.ui.idp.IdpSignInContainerActivity;
import com.firebase.ui.auth.util.CredentialsApiHelper;
import com.firebase.ui.auth.util.EmailFlowUtil;
import com.firebase.ui.auth.util.PlayServicesHelper;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import java.util.List;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.firebase.ui.auth.ui.ResultCodes.RESULT_NO_NETWORK;

/**
 * Attempts to acquire a credential from Smart Lock for Passwords to sign in
 * an existing account. If this succeeds, an attempt is made to sign the user in
 * with this credential. If it does not, the
 * {@link AuthMethodPickerActivity authentication method picker activity}
 * is started, unless only email is supported, in which case the
 * {@link SignInNoPasswordActivity email sign-in flow}
 * is started.
 */
public class SignInDelegate extends SmartLock<CredentialRequestResult> {
    private static final String TAG = "SignInDelegate";
    private static final int RC_CREDENTIALS_READ = 2;
    private static final int RC_IDP_SIGNIN = 3;
    private static final int RC_AUTH_METHOD_PICKER = 4;
    private static final int RC_EMAIL_FLOW = 5;
    private static final int RC_PLAY_SERVICES = 6;

    private Credential mCredential;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        if (!hasNetworkConnection()) {
            Log.d(TAG, "No network connection");
            finish(RESULT_NO_NETWORK, new Intent());
            return;
        }

        // Make Google Play Services available at the correct version, if possible
        boolean madeAvailable =
                PlayServicesHelper
                        .getInstance(getActivity())
                        .makePlayServicesAvailable(getActivity(), RC_PLAY_SERVICES,
                                                   new DialogInterface.OnCancelListener() {
                                                       @Override
                                                       public void onCancel(DialogInterface dialogInterface) {
                                                           Log.w(TAG,
                                                                 "playServices:dialog.onCancel()");
                                                           finish(RESULT_CANCELED, new Intent());
                                                       }
                                                   });

        if (!madeAvailable
                || !PlayServicesHelper.getInstance(getActivity()).isPlayServicesAvailable()) {
            Log.w(TAG, "playServices: could not make available.");
            finish(RESULT_CANCELED, new Intent());
            return;
        }

        if (mHelper.getFlowParams().smartLockEnabled) {
            mHelper.showLoadingDialog(R.string.progress_dialog_loading);
            initGoogleApiClient(null);
            mHelper.getCredentialsApi()
                    .request(mGoogleApiClient,
                             new CredentialRequest.Builder()
                                     .setPasswordLoginSupported(true)
                                     .setAccountTypes(IdentityProviders.GOOGLE,
                                                      IdentityProviders.FACEBOOK,
                                                      IdentityProviders.TWITTER)
                                     .build())
                    .setResultCallback(this);
        } else {
            startAuthMethodChoice();
        }
    }

    @Override
    public void onResult(@NonNull CredentialRequestResult result) {
        Status status = result.getStatus();

        if (status.isSuccess()) {
            // Auto sign-in success
            handleCredential(result.getCredential());
        } else if (status.hasResolution()
                && (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED
                || status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED)) {
            try {
                mHelper.startIntentSenderForResult(
                        status.getResolution().getIntentSender(),
                        RC_CREDENTIALS_READ);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Failed to send Credentials intent.", e);
                startAuthMethodChoice();
            }
        } else {
            startAuthMethodChoice();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_CREDENTIALS_READ:
                if (resultCode == RESULT_OK) {
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
            case RC_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    finish(resultCode, data);
                }
                break;
        }
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

    private void initGoogleApiClient(String accountName) {
        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();

        if (accountName != null) {
            gsoBuilder.setAccountName(accountName);
        }

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addApi(Auth.CREDENTIALS_API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gsoBuilder.build())
                .build();
        mGoogleApiClient.connect();
    }

    private void startAuthMethodChoice() {
        List<AuthUI.IdpConfig> idpConfigs = mHelper.getFlowParams().providerInfo;

        // If the only provider is Email, immediately launch the email flow. Otherwise, launch
        // the auth method picker screen.
        if (idpConfigs.size() == 1) {
            if (idpConfigs.get(0).getProviderId().equals(EmailAuthProvider.PROVIDER_ID)) {
                startActivityForResult(
                        EmailFlowUtil.createIntent(
                                getContext(),
                                mHelper.getFlowParams()),
                        RC_EMAIL_FLOW);
            } else {
                redirectToIdpSignIn(null,
                                    SmartLock.providerIdToAccountType(
                                            idpConfigs.get(0).getProviderId()));
            }
        } else {
            startActivityForResult(
                    AuthMethodPickerActivity.createIntent(
                            getContext(),
                            mHelper.getFlowParams()),
                    RC_AUTH_METHOD_PICKER);
        }
    }

    /**
     * Begin sign in process with email and password from a SmartLock credential.
     * On success, finish with {@code RESULT_OK}.
     * On failure, delete the credential from SmartLock (if applicable) and then launch the
     * auth method picker flow.
     */
    private void signInWithEmailAndPassword(String email, String password) {
        mHelper.getFirebaseAuth()
                .signInWithEmailAndPassword(email, password)
                .addOnFailureListener(new TaskFailureLogger(
                        TAG, "Error signing in with email and password"))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        finish(RESULT_OK, new Intent());
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

        CredentialsApiHelper credentialsApiHelper = CredentialsApiHelper.getInstance(getActivity());
        credentialsApiHelper.delete(mCredential)
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
        Intent nextIntent;
        if (accountType.equals(IdentityProviders.GOOGLE)
                || accountType.equals(IdentityProviders.FACEBOOK)
                || accountType.equals(IdentityProviders.TWITTER)) {
            nextIntent = IdpSignInContainerActivity.createIntent(
                    getContext(),
                    mHelper.getFlowParams(),
                    AuthCredentialHelper.accountTypeToProviderId(accountType),
                    email);
        } else {
            Log.w(TAG, "unknown provider: " + accountType);
            nextIntent = AuthMethodPickerActivity.createIntent(
                    getContext(),
                    mHelper.getFlowParams());
        }
        startActivityForResult(nextIntent, RC_IDP_SIGNIN);
    }

    private void finish(int resultCode, Intent data) {
        cleanup();

        try {
            if (resultCode == RESULT_OK) {
                ((AuthUI.SignInResult) getActivity()).onAuthUISuccess(data);
            } else {
                ((AuthUI.SignInResult) getActivity()).onAuthUIFailure(resultCode);
            }
        } catch (ClassCastException e) {
            Log.e(TAG, getActivity().toString()
                    + " must implement AuthUI.AuthUIResult to receive sign in results");
        }

        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
    }

    /**
     * Check if there is an active or soon-to-be-active network connection.
     */
    private boolean hasNetworkConnection() {
        ConnectivityManager manager =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        return manager != null
                && manager.getActiveNetworkInfo() != null
                && manager.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    public static void delegate(FragmentActivity activity, FlowParameters params) {
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        Fragment fragment = fm.findFragmentByTag(TAG);
        if (fragment == null || !(fragment instanceof SignInDelegate)) {
            SignInDelegate result = new SignInDelegate();
            result.setArguments(FragmentHelper.getFlowParamsBundle(params));
            ft.add(result, TAG).disallowAddToBackStack().commit();
        }
    }
}
