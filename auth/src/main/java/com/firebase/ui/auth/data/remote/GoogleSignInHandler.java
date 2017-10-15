package com.firebase.ui.auth.data.remote;

import android.app.Application;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.ActivityResult;
import com.firebase.ui.auth.data.model.ProviderErrorException;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.firebase.ui.auth.util.ui.ViewModelBase;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.GoogleAuthProvider;

public class GoogleSignInHandler extends ViewModelBase<GoogleSignInHandler.Params>
        implements Observer<ActivityResult> {
    private static final int RC_SIGN_IN = 13;

    private AuthUI.IdpConfig mConfig;
    private SignInHandler mHandler;
    private FlowHolder mFlowHolder;
    private GoogleApiClient mClient;

    public GoogleSignInHandler(Application application) {
        super(application);
    }

    private static IdpResponse createIdpResponse(GoogleSignInAccount account) {
        return new IdpResponse.Builder(
                new User.Builder(GoogleAuthProvider.PROVIDER_ID, account.getEmail())
                        .setName(account.getDisplayName())
                        .setPhotoUri(account.getPhotoUrl())
                        .build())
                .setToken(account.getIdToken())
                .build();
    }

    @Override
    protected void onCreate(Params params) {
        mConfig = params.providerConfig;
        mHandler = params.signInHandler;
        mFlowHolder = params.flowHolder;

        mFlowHolder.getActivityResultListener().observeForever(this);
        initClient(params.email);
    }

    public void start() {
        mFlowHolder.getIntentStarter().setValue(Pair.create(
                Auth.GoogleSignInApi.getSignInIntent(mClient),
                RC_SIGN_IN));
    }

    private void initClient(@Nullable String email) {
        mClient = new GoogleApiClient.Builder(getApplication())
                .addApi(Auth.GOOGLE_SIGN_IN_API, getSignInOptions(email))
                .build();
    }

    private GoogleSignInOptions getSignInOptions(@Nullable String email) {
        String clientId = getApplication().getString(R.string.default_web_client_id);

        GoogleSignInOptions.Builder builder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken(clientId);

        // Add additional scopes
        for (String scopeString : mConfig.getScopes()) {
            builder.requestScopes(new Scope(scopeString));
        }

        if (!TextUtils.isEmpty(email)) {
            builder.setAccountName(email);
        }

        return builder.build();
    }

    @Override
    public void onChanged(@Nullable ActivityResult result) {
        if (result.getRequestCode() == RC_SIGN_IN) {
            GoogleSignInResult signInResult =
                    Auth.GoogleSignInApi.getSignInResultFromIntent(result.getData());

            if (signInResult.isSuccess()) {
                mHandler.signIn(createIdpResponse(signInResult.getSignInAccount()));
            } else {
                Status status = signInResult.getStatus();

                if (status.getStatusCode() == CommonStatusCodes.INVALID_ACCOUNT) {
                    initClient(null);
                    start();
                } else {
                    mHandler.signIn(IdpResponse.fromError(new ProviderErrorException(
                            "Code: " + status.getStatusCode() + ", message: " + status.getStatusMessage())));
                }
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mFlowHolder.getActivityResultListener().removeObserver(this);
    }

    public static final class Params {
        public final AuthUI.IdpConfig providerConfig;
        public final SignInHandler signInHandler;
        public final FlowHolder flowHolder;
        @Nullable public final String email;

        public Params(AuthUI.IdpConfig config,
                      SignInHandler handler,
                      FlowHolder holder,
                      @Nullable String email) {
            providerConfig = config;
            signInHandler = handler;
            flowHolder = holder;
            this.email = email;
        }
    }
}
