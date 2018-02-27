package com.firebase.ui.auth.data.remote;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
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
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.SingleLiveEvent;
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
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.ArrayList;
import java.util.List;

public class SignInKickstarter extends AuthViewModelBase<IdpResponse> {
    private static final int RC_CREDENTIALS_READ = 2;
    private static final int RC_IDP_SIGNIN = 3;
    private static final int RC_AUTH_METHOD_PICKER = 4;
    private static final int RC_EMAIL_FLOW = 5;
    private static final int RC_PHONE_FLOW = 6;

    private MutableLiveData<Pair<Intent, Integer>> mIntentReqester = new SingleLiveEvent<>();
    private MutableLiveData<Pair<PendingIntent, Integer>> mPendingIntentReqester =
            new SingleLiveEvent<>();

    public SignInKickstarter(Application application) {
        super(application);
    }

    public LiveData<Pair<Intent, Integer>> getIntentReqester() {
        return mIntentReqester;
    }

    public LiveData<Pair<PendingIntent, Integer>> getPendingIntentReqester() {
        return mPendingIntentReqester;
    }

    public void start() {
        // Only support password credentials if email auth is enabled
        boolean supportPasswords = ProviderUtils.getConfigFromIdps(
                getArguments().providerInfo, EmailAuthProvider.PROVIDER_ID) != null;
        List<String> accountTypes = getSupportedAccountTypes();

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
                                    mPendingIntentReqester.setValue(Pair.create(
                                            e.getResolution(),
                                            RC_CREDENTIALS_READ));
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
        List<AuthUI.IdpConfig> idpConfigs = getArguments().providerInfo;

        // If there is only one provider selected, launch the flow directly
        if (idpConfigs.size() == 1) {
            AuthUI.IdpConfig firstIdpConfig = idpConfigs.get(0);
            String firstProvider = firstIdpConfig.getProviderId();
            switch (firstProvider) {
                case EmailAuthProvider.PROVIDER_ID:
                    mIntentReqester.setValue(Pair.create(
                            EmailActivity.createIntent(getApplication(), getArguments()),
                            RC_EMAIL_FLOW));
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    mIntentReqester.setValue(Pair.create(
                            PhoneActivity.createIntent(
                                    getApplication(), getArguments(), firstIdpConfig.getParams()),
                            RC_PHONE_FLOW));
                    break;
                default:
                    redirectToIdpSignIn(firstProvider, null);
                    break;
            }
        } else {
            mIntentReqester.setValue(Pair.create(
                    AuthMethodPickerActivity.createIntent(getApplication(), getArguments()),
                    RC_AUTH_METHOD_PICKER));
        }
    }

    private void redirectToIdpSignIn(String provider, String email) {
        if (TextUtils.isEmpty(provider)) {
            mIntentReqester.setValue(Pair.create(
                    EmailActivity.createIntent(getApplication(), getArguments(), email),
                    RC_EMAIL_FLOW));
            return;
        }

        if (provider.equals(GoogleAuthProvider.PROVIDER_ID)
                || provider.equals(FacebookAuthProvider.PROVIDER_ID)
                || provider.equals(TwitterAuthProvider.PROVIDER_ID)) {
            mIntentReqester.setValue(Pair.create(
                    SingleSignInActivity.createIntent(
                            getApplication(),
                            getArguments(),
                            new User.Builder(provider, email).build()),
                    RC_IDP_SIGNIN));
        } else {
            startAuthMethodChoice();
        }
    }

    private List<String> getSupportedAccountTypes() {
        List<String> accounts = new ArrayList<>();
        for (AuthUI.IdpConfig idpConfig : getArguments().providerInfo) {
            @AuthUI.SupportedProvider String providerId = idpConfig.getProviderId();
            if (providerId.equals(GoogleAuthProvider.PROVIDER_ID)
                    || providerId.equals(FacebookAuthProvider.PROVIDER_ID)
                    || providerId.equals(TwitterAuthProvider.PROVIDER_ID)
                    || providerId.equals(PhoneAuthProvider.PROVIDER_ID)) {
                accounts.add(ProviderUtils.providerIdToAccountType(providerId));
            }
        }
        return accounts;
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case RC_CREDENTIALS_READ:
                if (resultCode == Activity.RESULT_OK) {
                    handleCredential((Credential) data.getParcelableExtra(Credential.EXTRA_KEY));
                } else {
                    startAuthMethodChoice();
                }
                break;
            case RC_AUTH_METHOD_PICKER:
            case RC_IDP_SIGNIN:
            case RC_EMAIL_FLOW:
            case RC_PHONE_FLOW:
                IdpResponse response = IdpResponse.fromResultIntent(data);
                if (response == null) {
                    setResult(Resource.<IdpResponse>forFailure(new UserCancellationException()));
                } else if (response.isSuccessful()) {
                    setResult(Resource.forSuccess(response));
                } else {
                    setResult(Resource.<IdpResponse>forFailure(response.getError()));
                }
        }
    }

    private void handleCredential(final Credential credential) {
        String id = credential.getId();
        String password = credential.getPassword();
        if (TextUtils.isEmpty(password)) {
            String provider = ProviderUtils.accountTypeToProviderId(
                    String.valueOf(credential.getAccountType()));
            if (TextUtils.equals(provider, PhoneAuthProvider.PROVIDER_ID)) {
                Bundle args = new Bundle();
                args.putString(ExtraConstants.EXTRA_PHONE, id);
                mIntentReqester.setValue(Pair.create(
                        PhoneActivity.createIntent(
                                getApplication(),
                                getArguments(),
                                args),
                        RC_PHONE_FLOW));
            } else {
                redirectToIdpSignIn(provider, id);
            }
        } else {
            final IdpResponse response = new IdpResponse.Builder(
                    new User.Builder(EmailAuthProvider.PROVIDER_ID, id).build())
                    .setPassword(password)
                    .build();

            getAuth().signInWithEmailAndPassword(id, password)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult result) {
                            setResult(Resource.forSuccess(response));
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
                                        .delete(credential)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                startAuthMethodChoice();
                                            }
                                        });
                            } else {
                                startAuthMethodChoice();
                            }
                        }
                    });
        }
    }
}
