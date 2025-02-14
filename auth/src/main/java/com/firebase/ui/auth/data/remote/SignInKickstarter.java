package com.firebase.ui.auth.data.remote;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.IntentRequiredException;
import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.email.EmailActivity;
import com.firebase.ui.auth.ui.email.EmailLinkCatcherActivity;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.ui.idp.SingleSignInActivity;
import com.firebase.ui.auth.ui.phone.PhoneActivity;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.firebase.ui.auth.viewmodel.SignInViewModelBase;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.identity.Identity;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.firebase.ui.auth.AuthUI.EMAIL_LINK_PROVIDER;

public class SignInKickstarter extends SignInViewModelBase {
    private static final String TAG = "SignInKickstarter";

    public SignInKickstarter(Application application) {
        super(application);
    }

    public void start() {
        if (!TextUtils.isEmpty(getArguments().emailLink)) {
            setResult(Resource.forFailure(new IntentRequiredException(
                    EmailLinkCatcherActivity.createIntent(getApplication(), getArguments()),
                    RequestCodes.EMAIL_FLOW)));
            return;
        }
        
        startAuthMethodChoice();
    }

    private void startAuthMethodChoice() {
        if (!getArguments().shouldShowProviderChoice()) {
            AuthUI.IdpConfig firstIdpConfig = getArguments().getDefaultOrFirstProvider();
            String firstProvider = firstIdpConfig.getProviderId();
            switch (firstProvider) {
                case EMAIL_LINK_PROVIDER:
                case EmailAuthProvider.PROVIDER_ID:
                    setResult(Resource.forFailure(new IntentRequiredException(
                            EmailActivity.createIntent(getApplication(), getArguments()),
                            RequestCodes.EMAIL_FLOW)));
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    setResult(Resource.forFailure(new IntentRequiredException(
                            PhoneActivity.createIntent(getApplication(), getArguments(), firstIdpConfig.getParams()),
                            RequestCodes.PHONE_FLOW)));
                    break;
                default:
                    redirectSignIn(firstProvider, null);
                    break;
            }
        } else {
            setResult(Resource.forFailure(new IntentRequiredException(
                    AuthMethodPickerActivity.createIntent(getApplication(), getArguments()),
                    RequestCodes.AUTH_PICKER_FLOW)));
        }
    }

    private void redirectSignIn(String provider, String id) {
        switch (provider) {
            case EmailAuthProvider.PROVIDER_ID:
                setResult(Resource.forFailure(new IntentRequiredException(
                        EmailActivity.createIntent(getApplication(), getArguments(), id),
                        RequestCodes.EMAIL_FLOW)));
                break;
            case PhoneAuthProvider.PROVIDER_ID:
                Bundle args = new Bundle();
                args.putString(ExtraConstants.PHONE, id);
                setResult(Resource.forFailure(new IntentRequiredException(
                        PhoneActivity.createIntent(getApplication(), getArguments(), args),
                        RequestCodes.PHONE_FLOW)));
                break;
            default:
                setResult(Resource.forFailure(new IntentRequiredException(
                        SingleSignInActivity.createIntent(getApplication(), getArguments(),
                                new User.Builder(provider, id).build()),
                        RequestCodes.PROVIDER_FLOW)));
        }
    }

    private List<String> getCredentialAccountTypes() {
        List<String> accounts = new ArrayList<>();
        for (AuthUI.IdpConfig idpConfig : getArguments().providers) {
            @AuthUI.SupportedProvider String providerId = idpConfig.getProviderId();
            if (providerId.equals(GoogleAuthProvider.PROVIDER_ID)) {
                accounts.add(ProviderUtils.providerIdToAccountType(providerId));
            }
        }
        return accounts;
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case RequestCodes.CRED_HINT:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    try {
                        SignInClient signInClient = Identity.getSignInClient(getApplication());
                        SignInCredential credential = signInClient.getSignInCredentialFromIntent(data);
                        handleCredential(credential);
                    } catch (ApiException e) {
                        // Optionally log the error
                        startAuthMethodChoice();
                    }
                } else {
                    startAuthMethodChoice();
                }
                break;
            case RequestCodes.EMAIL_FLOW:
            case RequestCodes.AUTH_PICKER_FLOW:
            case RequestCodes.PHONE_FLOW:
            case RequestCodes.PROVIDER_FLOW:
                if (resultCode == RequestCodes.EMAIL_LINK_WRONG_DEVICE_FLOW ||
                    resultCode == RequestCodes.EMAIL_LINK_INVALID_LINK_FLOW) {
                    startAuthMethodChoice();
                    return;
                }
                IdpResponse response = IdpResponse.fromResultIntent(data);
                if (response == null) {
                    setResult(Resource.forFailure(new UserCancellationException()));
                } else if (response.isSuccessful()) {
                    setResult(Resource.forSuccess(response));
                } else if (response.getError().getErrorCode() ==
                        ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT) {
                    handleMergeFailure(response);
                } else {
                    setResult(Resource.forFailure(response.getError()));
                }
        }
    }

    /**
     * Minimal change: Adapted to work with the new SignInCredential.
     */
    private void handleCredential(final SignInCredential credential) {
        String id = credential.getId();
        String password = credential.getPassword();
        if (TextUtils.isEmpty(password)) {
            // Instead of checking accountType, check for a Google ID token.
            String googleIdToken = credential.getGoogleIdToken();
            if (!TextUtils.isEmpty(googleIdToken)) {
                final IdpResponse response = new IdpResponse.Builder(
                        new User.Builder(GoogleAuthProvider.PROVIDER_ID, id).build()).build();
                setResult(Resource.forLoading());
                getAuth().signInWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null))
                        .addOnSuccessListener(authResult -> handleSuccess(response, authResult))
                        .addOnFailureListener(e -> startAuthMethodChoice());
            } else {
                startAuthMethodChoice();
            }
        } else {
            final IdpResponse response = new IdpResponse.Builder(
                    new User.Builder(EmailAuthProvider.PROVIDER_ID, id).build()).build();
            setResult(Resource.forLoading());
            getAuth().signInWithEmailAndPassword(id, password)
                    .addOnSuccessListener(authResult -> handleSuccess(response, authResult))
                    .addOnFailureListener(e -> {
                        if (e instanceof FirebaseAuthInvalidUserException ||
                            e instanceof FirebaseAuthInvalidCredentialsException) {
                            // Minimal change: sign out using the new API (delete isn’t available).
                            Identity.getSignInClient(getApplication()).signOut();
                        }
                        startAuthMethodChoice();
                    });
        }
    }
}
