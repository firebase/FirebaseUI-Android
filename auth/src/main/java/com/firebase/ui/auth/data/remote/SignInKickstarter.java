package com.firebase.ui.auth.data.remote;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.IntentRequiredException;
import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.email.EmailActivity;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.ui.idp.SingleSignInActivity;
import com.firebase.ui.auth.ui.phone.PhoneActivity;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.firebase.ui.auth.viewmodel.SignInViewModelBase;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResponse;
import com.google.android.gms.common.api.ApiException;
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
import com.google.firebase.auth.GithubAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.ArrayList;
import java.util.List;

public class SignInKickstarter extends SignInViewModelBase {
    public SignInKickstarter(Application application) {
        super(application);
    }

    public void start() {
        // Only support password credentials if email auth is enabled
        boolean supportPasswords = ProviderUtils.getConfigFromIdps(
                getArguments().providers, EmailAuthProvider.PROVIDER_ID) != null;
        List<String> accountTypes = getCredentialAccountTypes();

        // If the request will be empty, avoid the step entirely
        boolean willRequestCredentials = supportPasswords || accountTypes.size() > 0;

        if (getArguments().enableCredentials && willRequestCredentials) {
            setResult(Resource.<IdpResponse>forLoading());

            GoogleApiUtils.getCredentialsClient(getApplication())
                    .request(new CredentialRequest.Builder()
                            .setPasswordLoginSupported(supportPasswords)
                            .setAccountTypes(accountTypes.toArray(new String[accountTypes.size()]))
                            .build())
                    .addOnCompleteListener(new OnCompleteListener<CredentialRequestResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<CredentialRequestResponse> task) {
                            try {
                                handleCredential(
                                        task.getResult(ApiException.class).getCredential());
                            } catch (ResolvableApiException e) {
                                if (e.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
                                    setResult(Resource.<IdpResponse>forFailure(
                                            new PendingIntentRequiredException(
                                                    e.getResolution(), RequestCodes.CRED_HINT)));
                                } else {
                                    startAuthMethodChoice();
                                }
                            } catch (ApiException e) {
                                startAuthMethodChoice();
                            }
                        }
                    });
        } else {
            startAuthMethodChoice();
        }
    }

    private void startAuthMethodChoice() {
        // If there is only one provider selected, launch the flow directly
        if (!getArguments().shouldShowProviderChoice()) {
            AuthUI.IdpConfig firstIdpConfig = getArguments().providers.get(0);
            String firstProvider = firstIdpConfig.getProviderId();
            switch (firstProvider) {
                case EmailAuthProvider.PROVIDER_ID:
                    setResult(Resource.<IdpResponse>forFailure(new IntentRequiredException(
                            EmailActivity.createIntent(getApplication(), getArguments()),
                            RequestCodes.EMAIL_FLOW)));
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    setResult(Resource.<IdpResponse>forFailure(new IntentRequiredException(
                            PhoneActivity.createIntent(
                                    getApplication(), getArguments(), firstIdpConfig.getParams()),
                            RequestCodes.PHONE_FLOW)));
                    break;
                default:
                    redirectSignIn(firstProvider, null);
                    break;
            }
        } else {
            setResult(Resource.<IdpResponse>forFailure(new IntentRequiredException(
                    AuthMethodPickerActivity.createIntent(getApplication(), getArguments()),
                    RequestCodes.AUTH_PICKER_FLOW)));
        }
    }

    private void redirectSignIn(String provider, String id) {
        switch (provider) {
            case EmailAuthProvider.PROVIDER_ID:
                setResult(Resource.<IdpResponse>forFailure(new IntentRequiredException(
                        EmailActivity.createIntent(getApplication(), getArguments(), id),
                        RequestCodes.EMAIL_FLOW)));
                break;
            case PhoneAuthProvider.PROVIDER_ID:
                Bundle args = new Bundle();
                args.putString(ExtraConstants.PHONE, id);
                setResult(Resource.<IdpResponse>forFailure(new IntentRequiredException(
                        PhoneActivity.createIntent(
                                getApplication(),
                                getArguments(),
                                args),
                        RequestCodes.PHONE_FLOW)));
                break;
            case GoogleAuthProvider.PROVIDER_ID:
            case FacebookAuthProvider.PROVIDER_ID:
            case TwitterAuthProvider.PROVIDER_ID:
            case GithubAuthProvider.PROVIDER_ID:
                setResult(Resource.<IdpResponse>forFailure(new IntentRequiredException(
                        SingleSignInActivity.createIntent(
                                getApplication(),
                                getArguments(),
                                new User.Builder(provider, id).build()),
                        RequestCodes.PROVIDER_FLOW)));
                break;
            default:
                startAuthMethodChoice();
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
                if (resultCode == Activity.RESULT_OK) {
                    handleCredential((Credential) data.getParcelableExtra(Credential.EXTRA_KEY));
                } else {
                    startAuthMethodChoice();
                }
                break;
            case RequestCodes.AUTH_PICKER_FLOW:
            case RequestCodes.EMAIL_FLOW:
            case RequestCodes.PHONE_FLOW:
            case RequestCodes.PROVIDER_FLOW:
                IdpResponse response = IdpResponse.fromResultIntent(data);
                if (response == null) {
                    setResult(Resource.<IdpResponse>forFailure(new UserCancellationException()));
                } else if (response.isSuccessful()) {
                    setResult(Resource.forSuccess(response));
                } else if (response.getError().getErrorCode() ==
                        ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT) {
                    handleMergeFailure(response);
                } else {
                    setResult(Resource.<IdpResponse>forFailure(response.getError()));
                }
        }
    }

    private void handleCredential(final Credential credential) {
        String id = credential.getId();
        String password = credential.getPassword();
        if (TextUtils.isEmpty(password)) {
            String identity = credential.getAccountType();
            if (identity == null) {
                startAuthMethodChoice();
            } else {
                redirectSignIn(
                        ProviderUtils.accountTypeToProviderId(credential.getAccountType()), id);
            }
        } else {
            final IdpResponse response = new IdpResponse.Builder(
                    new User.Builder(EmailAuthProvider.PROVIDER_ID, id).build()).build();

            setResult(Resource.<IdpResponse>forLoading());
            getAuth().signInWithEmailAndPassword(id, password)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult result) {
                            handleSuccess(response, result);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (e instanceof FirebaseAuthInvalidUserException
                                    || e instanceof FirebaseAuthInvalidCredentialsException) {
                                // In this case the credential saved in SmartLock was not
                                // a valid credential, we should delete it from SmartLock
                                // before continuing.
                                GoogleApiUtils.getCredentialsClient(getApplication())
                                        .delete(credential);
                            }
                            startAuthMethodChoice();
                        }
                    });
        }
    }
}
