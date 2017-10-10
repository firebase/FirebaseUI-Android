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
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.email.RegisterEmailActivity;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.ui.idp.SingleSignInActivity;
import com.firebase.ui.auth.ui.phone.PhoneVerificationActivity;
import com.firebase.ui.auth.util.data.AuthViewModelBase;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.data.remote.InternalGoogleApiConnector;
import com.firebase.ui.auth.util.ui.ActivityResult;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
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
            new CredentialRequestFlow().start();
        } else {
            startAuthMethodChoice();
        }
    }

    private void startAuthMethodChoice() {
        FlowParameters flowParams = mFlowHolder.getParams();
        List<AuthUI.IdpConfig> idpConfigs = flowParams.providerInfo;

        // If there is only one provider selected, launch the flow directly
        if (idpConfigs.size() == 1) {
            AuthUI.IdpConfig firstIdpConfig = idpConfigs.get(0);
            String firstProvider = firstIdpConfig.getProviderId();
            switch (firstProvider) {
                case EmailAuthProvider.PROVIDER_ID:
                    mFlowHolder.getIntentStarter().setValue(Pair.create(
                            RegisterEmailActivity.createIntent(getApplication(), flowParams),
                            RC_EMAIL_FLOW));
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    mFlowHolder.getIntentStarter().setValue(Pair.create(
                            PhoneVerificationActivity.createIntent(
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
        FlowParameters flowParams = mFlowHolder.getParams();

        if (TextUtils.isEmpty(provider)) {
            mFlowHolder.getIntentStarter().setValue(Pair.create(
                    RegisterEmailActivity.createIntent(getApplication(), flowParams, email),
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

    private class CredentialRequestFlow extends InternalGoogleApiConnector
            implements ResultCallback<CredentialRequestResult> {
        protected CredentialRequestFlow() {
            super(new GoogleApiClient.Builder(getApplication())
                    .addApi(Auth.CREDENTIALS_API), mFlowHolder);
        }

        public void start() {
            connect();
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Auth.CredentialsApi.request(
                    mClient,
                    new CredentialRequest.Builder()
                            .setPasswordLoginSupported(true)
                            .setAccountTypes(getSupportedAccountTypes().toArray(new String[0]))
                            .build())
                    .setResultCallback(this);
        }

        private List<String> getSupportedAccountTypes() {
            List<String> accounts = new ArrayList<>();
            for (AuthUI.IdpConfig idpConfig : mFlowHolder.getParams().providerInfo) {
                @AuthUI.SupportedProvider String providerId = idpConfig.getProviderId();
                if (providerId.equals(GoogleAuthProvider.PROVIDER_ID)
                        || providerId.equals(FacebookAuthProvider.PROVIDER_ID)
                        || providerId.equals(TwitterAuthProvider.PROVIDER_ID)) {
                    accounts.add(ProviderUtils.providerIdToAccountType(providerId));
                }
            }
            return accounts;
        }

        @Override
        public void onResult(@NonNull CredentialRequestResult result) {
            Status status = result.getStatus();

            if (status.isSuccess()) {
                handleCredential(result.getCredential());
            } else if (status.hasResolution()
                    && status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
                mFlowHolder.getPendingIntentStarter().setValue(Pair.create(
                        status.getResolution(),
                        RC_CREDENTIALS_READ));
                return;
            } else {
                startAuthMethodChoice();
            }

            disconnect();
        }

        private void handleCredential(Credential credential) {
            String email = credential.getId();
            String password = credential.getPassword();
            if (TextUtils.isEmpty(email)) {
                startAuthMethodChoice();
            } else if (TextUtils.isEmpty(password)) {
                redirectToIdpSignIn(ProviderUtils.accountTypeToProviderId(
                        String.valueOf(credential.getAccountType())), email);
            } else {
                mHandler.start(new IdpResponse.Builder(
                        new User.Builder(EmailAuthProvider.PROVIDER_ID, email).build())
                        .setPassword(password)
                        .build());
            }
        }

        @Override
        public void onChanged(@Nullable ActivityResult result) {
            super.onChanged(result);
            if (result.getRequestCode() == RC_CREDENTIALS_READ) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    handleCredential(
                            (Credential) result.getData().getParcelableExtra(Credential.EXTRA_KEY));
                } else {
                    startAuthMethodChoice();
                }

                disconnect();
            }
        }

        @Override
        protected void onConnectionFailedIrreparably() {
            startAuthMethodChoice();
        }
    }
}
