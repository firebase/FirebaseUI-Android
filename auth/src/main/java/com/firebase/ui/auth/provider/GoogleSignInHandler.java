package com.firebase.ui.auth.provider;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.User;
import com.firebase.ui.auth.util.ActivityResult;
import com.firebase.ui.auth.util.FlowHolder;
import com.firebase.ui.auth.util.SignInFailedException;
import com.firebase.ui.auth.util.SignInHandler;
import com.firebase.ui.auth.util.SingleLiveEvent;
import com.firebase.ui.auth.util.ViewModelBase;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.GoogleAuthProvider;

public class GoogleSignInHandler extends ViewModelBase<GoogleSignInHandler.Params>
        implements Observer<ActivityResult> {
    public static final int RC_SIGN_IN = 20;

    private AuthUI.IdpConfig mConfig;
    private SignInHandler mHandler;
    private FlowHolder mFlowHolder;

    private final SingleLiveEvent<Status> mSignInFailedNotifier = new SingleLiveEvent<>();

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

        mFlowHolder.getOnActivityResult().observeForever(this);
    }

    public GoogleSignInOptions getSignInOptions(@Nullable String email) {
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

    public LiveData<Status> getSignInFailedNotifier() {
        return mSignInFailedNotifier;
    }

    @Override
    public void onChanged(@Nullable ActivityResult result) {
        if (result.getRequestCode() == RC_SIGN_IN) {
            GoogleSignInResult signInResult =
                    Auth.GoogleSignInApi.getSignInResultFromIntent(result.getData());

            if (signInResult.isSuccess()) {
                finish(Tasks.forResult(createIdpResponse(signInResult.getSignInAccount())));
            } else {
                Status status = signInResult.getStatus();

                mSignInFailedNotifier.setValue(status);
                if (status.getStatusCode() != CommonStatusCodes.INVALID_ACCOUNT) {
                    finish(Tasks.<IdpResponse>forException(new SignInFailedException(
                            String.valueOf(status.getStatusCode()),
                            String.valueOf(status.getStatusMessage()))));
                }
            }
        }
    }

    private void finish(Task<IdpResponse> task) {
        mFlowHolder.getOnActivityResult().removeObserver(this);
        mHandler.start(task);
    }

    public static final class Params {
        public final AuthUI.IdpConfig providerConfig;
        public final SignInHandler signInHandler;
        public final FlowHolder flowHolder;

        public Params(AuthUI.IdpConfig config, SignInHandler handler, FlowHolder holder) {
            providerConfig = config;
            signInHandler = handler;
            flowHolder = holder;
        }
    }
}
