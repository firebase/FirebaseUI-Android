package com.firebase.ui.auth.data.remote;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.ActivityResult;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.email.EmailActivity;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.ui.idp.SingleSignInActivity;
import com.firebase.ui.auth.ui.phone.PhoneNumberActivity;
import com.firebase.ui.auth.util.data.AuthViewModelBase;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResponse;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.ArrayList;
import java.util.List;

public class SignInKickstarter extends AuthViewModelBase implements Observer<ActivityResult> {
    private static final int RC_CREDENTIALS_READ = 2;
    private static final int RC_IDP_SIGNIN = 3;
    private static final int RC_AUTH_METHOD_PICKER = 4;
    private static final int RC_EMAIL_FLOW = 5;
    private static final int RC_PHONE_FLOW = 6;

    private SignInHandler mHandler;

    public SignInKickstarter(Application application) {
        super(application);
    }

    @Override
    protected void onCreate(FlowHolder args) {
        super.onCreate(args);
        mFlowHolder.getActivityResultListener().observeForever(this);
    }

    public void setSignInHandler(SignInHandler handler) {
        mHandler = handler;
    }

    public void start() {
        if (mFlowHolder.getParams().enableCredentials) {
            mFlowHolder.getProgressListener().setValue(false);

            Credentials.getClient(getApplication()).request(
                    new CredentialRequest.Builder()
                            .setPasswordLoginSupported(true)
                            .setAccountTypes(getSupportedAccountTypes().toArray(new String[0]))
                            .build())
                    .addOnCompleteListener(new CredentialRequestFlow());
        } else {
            startAuthMethodChoice();
        }
    }

    private void startAuthMethodChoice() {
        mFlowHolder.getProgressListener().setValue(true);

        FlowParameters flowParams = mFlowHolder.getParams();
        List<AuthUI.IdpConfig> idpConfigs = flowParams.providerInfo;

        // If there is only one provider selected, launch the flow directly
        if (idpConfigs.size() == 1) {
            AuthUI.IdpConfig firstIdpConfig = idpConfigs.get(0);
            String firstProvider = firstIdpConfig.getProviderId();
            switch (firstProvider) {
                case EmailAuthProvider.PROVIDER_ID:
                    mFlowHolder.getIntentStarter().setValue(Pair.create(
                            EmailActivity.createIntent(getApplication(), flowParams),
                            RC_EMAIL_FLOW));
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    mFlowHolder.getIntentStarter().setValue(Pair.create(
                            PhoneNumberActivity.createIntent(
                                    getApplication(), flowParams, firstIdpConfig.getParams()),
                            RC_PHONE_FLOW));
                    break;
                default:
                    redirectToIdpSignIn(firstProvider, null);
                    break;
            }
        } else {
            mFlowHolder.getIntentStarter().setValue(Pair.create(
                    AuthMethodPickerActivity.createIntent(getApplication(), flowParams),
                    RC_AUTH_METHOD_PICKER));
        }
    }

    private void redirectToIdpSignIn(String provider, String email) {
        mFlowHolder.getProgressListener().setValue(true);

        FlowParameters flowParams = mFlowHolder.getParams();

        if (TextUtils.isEmpty(provider)) {
            mFlowHolder.getIntentStarter().setValue(Pair.create(
                    EmailActivity.createIntent(getApplication(), flowParams, email),
                    RC_EMAIL_FLOW));
            return;
        }

        if (provider.equals(GoogleAuthProvider.PROVIDER_ID)
                || provider.equals(FacebookAuthProvider.PROVIDER_ID)
                || provider.equals(TwitterAuthProvider.PROVIDER_ID)) {
            mFlowHolder.getIntentStarter().setValue(Pair.create(
                    SingleSignInActivity.createIntent(
                            getApplication(),
                            flowParams,
                            new User.Builder(provider, email).build()),
                    RC_IDP_SIGNIN));
        } else {
            startAuthMethodChoice();
        }
    }

    private List<String> getSupportedAccountTypes() {
        List<String> accounts = new ArrayList<>();
        for (AuthUI.IdpConfig idpConfig : mFlowHolder.getParams().providerInfo) {
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

    @Override
    public void onChanged(@Nullable ActivityResult result) {
        switch (result.getRequestCode()) {
            case RC_AUTH_METHOD_PICKER:
            case RC_IDP_SIGNIN:
            case RC_EMAIL_FLOW:
            case RC_PHONE_FLOW:
                @Nullable IdpResponse response = IdpResponse.fromResultIntent(result.getData());
                SIGN_IN_LISTENER.setValue(response == null ?
                        IdpResponse.fromError(new UserCancellationException("User pressed back"))
                        : response);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mFlowHolder.getActivityResultListener().removeObserver(this);
    }

    private class CredentialRequestFlow implements OnCompleteListener<CredentialRequestResponse>,
            Observer<ActivityResult> {
        public CredentialRequestFlow() {
            mFlowHolder.getActivityResultListener().observeForever(this);
        }

        @Override
        public void onComplete(@NonNull Task<CredentialRequestResponse> task) {
            try {
                handleCredential(task.getResult(ApiException.class).getCredential());
            } catch (ResolvableApiException e) {
                mFlowHolder.getPendingIntentStarter().setValue(Pair.create(
                        e.getResolution(),
                        RC_CREDENTIALS_READ));
                return;
            } catch (ApiException e) {
                startAuthMethodChoice();
            }

            mFlowHolder.getActivityResultListener().removeObserver(this);
        }

        private void handleCredential(Credential credential) {
            String id = credential.getId();
            String password = credential.getPassword();
            if (TextUtils.isEmpty(password)) {
                String provider = ProviderUtils.accountTypeToProviderId(
                        String.valueOf(credential.getAccountType()));
                if (TextUtils.equals(provider, PhoneAuthProvider.PROVIDER_ID)) {
                    mFlowHolder.getProgressListener().setValue(true);

                    Bundle args = new Bundle();
                    args.putString(AuthUI.EXTRA_DEFAULT_PHONE_NUMBER, id);
                    mFlowHolder.getIntentStarter().setValue(Pair.create(
                            PhoneNumberActivity.createIntent(
                                    getApplication(),
                                    mFlowHolder.getParams(),
                                    args),
                            RC_PHONE_FLOW));
                } else {
                    redirectToIdpSignIn(provider, id);
                }
            } else {
                mHandler.signIn(new IdpResponse.Builder(
                        new User.Builder(EmailAuthProvider.PROVIDER_ID, id).build())
                        .setPassword(password)
                        .build());
            }
        }

        @Override
        public void onChanged(@Nullable ActivityResult result) {
            if (result.getRequestCode() == RC_CREDENTIALS_READ) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    handleCredential(
                            (Credential) result.getData().getParcelableExtra(Credential.EXTRA_KEY));
                } else {
                    startAuthMethodChoice();
                }

                mFlowHolder.getActivityResultListener().removeObserver(this);
            }
        }
    }
}
