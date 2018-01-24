package com.firebase.ui.auth.util.signincontainer;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.EmailActivity;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.ui.phone.PhoneActivity;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResponse;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Attempts to acquire a credential from Smart Lock for Passwords to sign in an existing account. If
 * this succeeds, an attempt is made to sign the user in with this credential. If it does not, the
 * {@link AuthMethodPickerActivity authentication method picker activity} is started, unless only
 * email is supported, in which case the {@link EmailActivity} is started.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SignInDelegate extends FragmentBase
        implements OnCompleteListener<CredentialRequestResponse> {

    private static final String TAG = "SignInDelegate";

    private static final int RC_CREDENTIALS_READ = 2;
    private static final int RC_IDP_SIGNIN = 3;
    private static final int RC_AUTH_METHOD_PICKER = 4;
    private static final int RC_EMAIL_FLOW = 5;
    private static final int RC_PHONE_FLOW = 6;

    private boolean mWasProgressDialogShowing;
    private Pair<Integer, Intent> mActivityResultPair;

    private Credential mCredential;
    private CredentialsClient mCredentialsClient;

    public static void delegate(FragmentActivity activity, FlowParameters params) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (!(fragment instanceof SignInDelegate)) {
            SignInDelegate result = new SignInDelegate();
            result.setArguments(params.toBundle());
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
        setRetainInstance(true);

        if (savedInstance != null) {
            // We already have a running instance of this fragment
            return;
        }

        FlowParameters flowParams = getFlowParams();

        // Only support password credentials if email auth is enabled
        boolean supportPasswords = false;
        for (AuthUI.IdpConfig config : flowParams.providerInfo) {
            if (EmailAuthProvider.PROVIDER_ID.equals(config.getProviderId())) {
                supportPasswords = true;
            }
        }
        List<String> accountTypes = getSupportedAccountTypes();

        // If the request will be empty, avoid the step entirely
        boolean willRequestCredentials = supportPasswords || accountTypes.size() > 0;

        if (flowParams.enableCredentials && willRequestCredentials) {
            getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_loading);

            mCredentialsClient = GoogleApiUtils.getCredentialsClient(getActivity());
            mCredentialsClient.request(new CredentialRequest.Builder()
                    .setPasswordLoginSupported(supportPasswords)
                    .setAccountTypes(accountTypes.toArray(new String[accountTypes.size()]))
                    .build())
                    .addOnCompleteListener(this);
        } else {
            startAuthMethodChoice();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        if (mActivityResultPair != null) {
            finish(mActivityResultPair.first, mActivityResultPair.second);
        } else if (mWasProgressDialogShowing) {
            getDialogHolder().showLoadingDialog(com.firebase.ui.auth.R.string.fui_progress_dialog_loading);
            mWasProgressDialogShowing = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mWasProgressDialogShowing = getDialogHolder().isProgressDialogShowing();
        getDialogHolder().dismissDialog();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // It doesn't matter what we put here, we just don't want outState to be empty
        outState.putBoolean(ExtraConstants.HAS_EXISTING_INSTANCE, true);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onComplete(@NonNull Task<CredentialRequestResponse> task) {

        if (task.isSuccessful()) {
            // Auto sign-in success
            handleCredential(task.getResult().getCredential());
            return;
        } else if (task.getException() instanceof ResolvableApiException) {
            ResolvableApiException rae = (ResolvableApiException) task.getException();
            if (rae.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
                try {
                    startIntentSenderForResult(rae.getResolution().getIntentSender(),
                            RC_CREDENTIALS_READ);
                    return;
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Failed to send Credentials intent.", e);
                }
            }
        } else {
            Log.e(TAG, "Non-resolvable exception:\n" + task.getException());
        }
        startAuthMethodChoice();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_CREDENTIALS_READ:
                if (resultCode == Activity.RESULT_OK) {
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
            case RC_PHONE_FLOW:
                finish(resultCode, data);
                break;
            default:
                IdpSignInContainer signInContainer = IdpSignInContainer.getInstance(getActivity());
                if (signInContainer != null) {
                    signInContainer.onActivityResult(requestCode, resultCode, data);
                }
        }
    }

    @Override
    public void finish(int resultCode, Intent resultIntent) {
        if (getActivity() == null) {
            // Because this fragment lives beyond the activity lifecycle, Fragment#getActivity()
            // might return null and we'll throw a NPE. To get around this, we wait until the
            // activity comes back to life in onStart and we finish it there.
            mActivityResultPair = new Pair<>(resultCode, resultIntent);
        } else {
            super.finish(resultCode, resultIntent);
        }
    }

    private List<String> getSupportedAccountTypes() {
        List<String> accounts = new ArrayList<>();
        for (AuthUI.IdpConfig idpConfig : getFlowParams().providerInfo) {
            @AuthUI.SupportedProvider String providerId = idpConfig.getProviderId();
            if (providerId.equals(GoogleAuthProvider.PROVIDER_ID)
                    || providerId.equals(FacebookAuthProvider.PROVIDER_ID)
                    || providerId.equals(TwitterAuthProvider.PROVIDER_ID)) {
                accounts.add(ProviderUtils.providerIdToAccountType(providerId));
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
        FlowParameters flowParams = getFlowParams();
        List<AuthUI.IdpConfig> idpConfigs = flowParams.providerInfo;

        // If there is only one provider selected, launch the flow directly
        if (idpConfigs.size() == 1) {
            AuthUI.IdpConfig firstIdpConfig = idpConfigs.get(0);
            String firstProvider = firstIdpConfig.getProviderId();
            switch (firstProvider) {
                case EmailAuthProvider.PROVIDER_ID:
                    // Go directly to email flow
                    startActivityForResult(
                            EmailActivity.createIntent(getContext(), flowParams),
                            RC_EMAIL_FLOW);
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    // Go directly to phone flow
                    Bundle params = firstIdpConfig.getParams();
                    Intent phoneActivityIntent = PhoneActivity
                            .createIntent(getContext(), flowParams, params);
                    startActivityForResult(
                            phoneActivityIntent,
                            RC_PHONE_FLOW);
                    break;
                default:
                    // Launch IDP flow
                    redirectToIdpSignIn(null, ProviderUtils.providerIdToAccountType(firstProvider));
                    break;
            }
        } else {
            startActivityForResult(
                    AuthMethodPickerActivity.createIntent(
                            getContext(),
                            flowParams),
                    RC_AUTH_METHOD_PICKER);
        }
        getDialogHolder().dismissDialog();
    }

    /**
     * Begin sign in process with email and password from a SmartLock credential. On success, finish
     * with {@link Activity#RESULT_OK}. On failure, delete the credential from SmartLock (if
     * applicable) and then launch the auth method picker flow.
     */
    private void signInWithEmailAndPassword(String email, String password) {
        final IdpResponse response =
                new IdpResponse.Builder(new User.Builder(EmailAuthProvider.PROVIDER_ID, email).build())
                        .build();

        getAuthHelper().getFirebaseAuth()
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        finish(Activity.RESULT_OK, response.toIntent());
                    }
                })
                .addOnFailureListener(new TaskFailureLogger(
                        TAG, "Error signing in with email and password"))
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof FirebaseAuthInvalidUserException
                                || e instanceof FirebaseAuthInvalidCredentialsException) {
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
    private void deleteCredentialAndRedirect() {
        if (mCredential == null) {
            Log.w(TAG, "deleteCredentialAndRedirect: null credential");
            startAuthMethodChoice();
            return;
        }

        GoogleApiUtils.getCredentialsClient(getActivity()).delete(mCredential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
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
                    EmailActivity.createIntent(
                            getContext(),
                            getFlowParams(),
                            email),
                    RC_EMAIL_FLOW);
            return;
        }

        if (accountType.equals(IdentityProviders.GOOGLE)
                || accountType.equals(IdentityProviders.FACEBOOK)
                || accountType.equals(IdentityProviders.TWITTER)) {
            IdpSignInContainer.signIn(
                    getActivity(),
                    getFlowParams(),
                    new User.Builder(ProviderUtils.accountTypeToProviderId(accountType), email)
                            .build());
        } else {
            Log.w(TAG, "Unknown provider: " + accountType);
            startActivityForResult(
                    AuthMethodPickerActivity.createIntent(
                            getContext(),
                            getFlowParams()),
                    RC_IDP_SIGNIN);
            getDialogHolder().dismissDialog();
        }
    }
}
