package com.firebase.ui.auth.util;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.provider.IDPProviderParcel;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.ui.idp.IDPSignInContainerActivity;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.List;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;

public class SignInDelegate extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        ResultCallback<CredentialRequestResult>,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "SignInDelegate";
    private static final int RC_CREDENTIALS_READ = 2;
    private static final int RC_IDP_SIGNIN = 3;
    private static final int RC_AUTH_METHOD_PICKER = 4;
    private static final int RC_EMAIL_FLOW = 5;
    private static final int RC_PLAY_SERVICES = 6;

    private Activity mActivity;
    private GoogleApiClient mGoogleApiClient;
    private ProgressDialog mProgressDialog;
    private CredentialRequestResult mCredentialRequestResult;
    private Credential mCredential;
    private FlowParameters mFlowParams;
    private AuthUI.AuthUIResult mAuthUIResult;

    @Override
    public void onConnectionSuspended(int i) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Connection suspended with code " + i);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "connection failed with " + connectionResult.getErrorMessage()
                    + " and code: " + connectionResult.getErrorCode());
        }
        Toast.makeText(mActivity, "An error has occurred.", Toast.LENGTH_SHORT).show();
    }

    private void initGoogleApiClient(String accountName) {
        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();

        if (accountName != null) {
            gsoBuilder.setAccountName(accountName);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                .addConnectionCallbacks(this)
                .addApi(Auth.CREDENTIALS_API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gsoBuilder.build())
                .build();
    }

    private void showProgress() {
        if (mProgressDialog == null || !mProgressDialog.isShowing()) {
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(
                    mActivity.getString(R.string.progress_dialog_loading));
        }
        mProgressDialog.show();
    }

    private void hideProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public static SignInDelegate newInstance(Activity activity,
                                             AuthUI.AuthUIResult authUIResult,
                                             FlowParameters parameters) {
        SignInDelegate result;

        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        Fragment fragment = fm.findFragmentByTag(TAG);
        if (fragment == null || !(fragment instanceof SignInDelegate)) {
            result = new SignInDelegate();

            result.mActivity = activity;
            result.mAuthUIResult = authUIResult;
            result.mFlowParams = parameters;

            ft.add(result, TAG).disallowAddToBackStack().commit();
        } else {
            result = (SignInDelegate) fragment;
        }

        return result;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        // Make Google Play Services available at the correct version, if possible
        boolean madeAvailable =
                PlayServicesHelper
                        .getInstance(mActivity)
                        .makePlayServicesAvailable(mActivity, RC_PLAY_SERVICES,
                                                   new DialogInterface.OnCancelListener() {
                                                       @Override
                                                       public void onCancel(DialogInterface dialogInterface) {
                                                           Log.w(TAG,
                                                                 "playServices:dialog.onCancel()");
                                                           mAuthUIResult.onResult(RESULT_CANCELED,
                                                                                  new Intent());
                                                       }
                                                   });

        if (!madeAvailable
                || !PlayServicesHelper.getInstance(mActivity).isPlayServicesAvailable()
                || !FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(mFlowParams.appName)
                .isPlayServicesAvailable(mActivity)) {
            Log.w(TAG, "playServices: could not make available.");
            mAuthUIResult.onResult(RESULT_CANCELED, new Intent());
            return;
        }

        if (!mFlowParams.smartLockEnabled) {
            startAuthMethodChoice();
        } else {
            showProgress();
            initGoogleApiClient(null);
            Auth.CredentialsApi
                    .request(mGoogleApiClient,
                             new CredentialRequest.Builder()
                                     .setPasswordLoginSupported(true)
                                     .setAccountTypes(IdentityProviders.GOOGLE).build())
                    .setResultCallback(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    private void logInWithCredential(
            String email,
            String password,
            String accountType) {
        if (email != null) {
            if (password != null && !password.isEmpty()) {
                // email/password combination
                signInWithEmailAndPassword(email, password);
            } else {
                // identifier/provider combination
                redirectToIdpSignIn(email, accountType);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
        }

        // TODO: 10/15/2016

        hideProgress();

        switch (requestCode) {
            case RC_CREDENTIALS_READ:
                if (resultCode == RESULT_OK) {
                    // credential selected from SmartLock, log in with that credential
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    handleCredential(credential);
                    logInWithCredential(getEmailFromCredential(),
                                        getPasswordFromCredential(),
                                        getAccountTypeFromCredential());
                } else if (resultCode == RESULT_CANCELED
                        || resultCode == CredentialsApi.ACTIVITY_RESULT_OTHER_ACCOUNT) {
                    // Smart lock selector cancelled, go to the AuthMethodPicker screen
                    startAuthMethodChoice();
                } else if (resultCode == RESULT_FIRST_USER) {
                    // TODO: (serikb) figure out flow
                }
                break;
            case RC_IDP_SIGNIN:
            case RC_AUTH_METHOD_PICKER:
            case RC_EMAIL_FLOW:
                mAuthUIResult.onResult(resultCode, new Intent());
                break;
            case RC_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mAuthUIResult.onResult(resultCode, new Intent());
                }
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onResult(@NonNull CredentialRequestResult result) {
        mCredentialRequestResult = result;
        Status status = result.getStatus();

        if (status.isSuccess()) {
            // Auto sign-in success
            handleCredential(result.getCredential());
            delegateSignIn(true, false);
        } else if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
            delegateSignIn(false, true);
        }
        hideProgress();
    }

    private void delegateSignIn(boolean isAutoSignInAvailable, boolean isSignInResolutionNeeded) {
        String email = getEmailFromCredential();
        String password = getPasswordFromCredential();

        // Attempt auto-sign in using SmartLock
        if (isAutoSignInAvailable) {
            googleSilentSignIn();
            if (!TextUtils.isEmpty(password)) {
                // Sign in with the email/password retrieved from SmartLock
                signInWithEmailAndPassword(email, password);
            } else {
                // log in with id/provider
                redirectToIdpSignIn(email, getAccountTypeFromCredential());
            }
        } else if (isSignInResolutionNeeded) {
            // resolve credential
            resolveSavedEmails();
        } else {
            startAuthMethodChoice();
        }
    }

    private void resolveSavedEmails() {
        if (mCredentialRequestResult == null || mCredentialRequestResult.getStatus() == null) {
            return;
        }
        Status status = mCredentialRequestResult.getStatus();
        if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
            try {
                // TODO check the mactcity stuff
                status.startResolutionForResult(mActivity, RC_CREDENTIALS_READ);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Failed to send Credentials intent.", e);
            }
        }
    }

    private void handleCredential(Credential credential) {
        mCredential = credential;

        if (IdentityProviders.GOOGLE.equals(credential.getAccountType())) {
            // Google account, rebuild GoogleApiClient to set account name and then try
            initGoogleApiClient(credential.getId());
            Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        } else {
            // Email/password account
            String status = String.format("Signed in as %s", credential.getId());
            Log.d(TAG, status);
        }
    }

    private void googleSilentSignIn() {
        // Try silent sign-in with Google Sign In API
        Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
    }

    /**
     * Begin sign in process with email and password from a SmartLock credential.
     * On success, finish with {@code RESULT_OK}.
     * On failure, delete the credential from SmartLock (if applicable) and then launch the
     * auth method picker flow.
     */
    private void signInWithEmailAndPassword(String email, String password) {
        FirebaseAuth.getInstance(FirebaseApp.getInstance(mFlowParams.appName))
                .signInWithEmailAndPassword(email, password)
                .addOnFailureListener(new TaskFailureLogger(
                        TAG, "Error signing in with email and password"))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        mAuthUIResult.onResult(RESULT_OK, new Intent());
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

        CredentialsApiHelper credentialsApiHelper = CredentialsApiHelper.getInstance(mActivity);
        credentialsApiHelper.delete(mCredential)
                .addOnCompleteListener(mActivity, new OnCompleteListener<Status>() {
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
        switch (accountType) {
            case IdentityProviders.GOOGLE:
                nextIntent = IDPSignInContainerActivity.createIntent(
                        mActivity,
                        mFlowParams,
                        GoogleAuthProvider.PROVIDER_ID,
                        email);
                break;
            case IdentityProviders.FACEBOOK:
                nextIntent = IDPSignInContainerActivity.createIntent(
                        mActivity,
                        mFlowParams,
                        FacebookAuthProvider.PROVIDER_ID,
                        email);
                break;
            default:
                Log.w(TAG, "unknown provider: " + accountType);
                nextIntent = AuthMethodPickerActivity.createIntent(
                        mActivity,
                        mFlowParams);
        }
        this.startActivityForResult(nextIntent, RC_IDP_SIGNIN);
    }

    private void startAuthMethodChoice() {
        List<IDPProviderParcel> providers = mFlowParams.providerInfo;

        // If the only provider is Email, immediately launch the email flow. Otherwise, launch
        // the auth method picker screen.
        if (providers.size() == 1
                && providers.get(0).getProviderType().equals(EmailAuthProvider.PROVIDER_ID)) {
            startActivityForResult(
                    EmailFlowUtil.createIntent(
                            mActivity,
                            mFlowParams),
                    RC_EMAIL_FLOW);
        } else {
            startActivityForResult(
                    // TODO test this getcontext stuff
                    AuthMethodPickerActivity.createIntent(
                            mActivity,
                            mFlowParams),
                    RC_AUTH_METHOD_PICKER);
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
}
