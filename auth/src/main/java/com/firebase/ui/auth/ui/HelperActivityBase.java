package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.AuthHelper;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.FlowHolder;
import com.firebase.ui.auth.viewmodel.PendingResolution;
import com.firebase.ui.auth.viewmodel.smartlock.SmartLockViewModel;
import com.google.firebase.auth.FirebaseUser;

import static com.firebase.ui.auth.util.Preconditions.checkNotNull;

@SuppressWarnings("Registered")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HelperActivityBase extends AppCompatActivity {

    private static final String TAG = "HelperActivityBase";

    private FlowHolder mFlowHolder;

    private FlowParameters mFlowParameters;
    private AuthHelper mAuthHelper;
    private ProgressDialogHolder mProgressDialogHolder;

    private IdpResponse mPendingIdpResponse;
    private SmartLockViewModel mSmartLockViewModel;

    public static Intent createBaseIntent(
            @NonNull Context context,
            @NonNull Class<? extends Activity> target,
            @NonNull FlowParameters flowParams) {
        return new Intent(
                checkNotNull(context, "context cannot be null"),
                checkNotNull(target, "target activity cannot be null"))
                .putExtra(ExtraConstants.EXTRA_FLOW_PARAMS,
                        checkNotNull(flowParams, "flowParams cannot be null"));
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        if (savedInstance != null) {
            mPendingIdpResponse = savedInstance.getParcelable(ExtraConstants.EXTRA_IDP_RESPONSE);
        }

        mAuthHelper = new AuthHelper(getFlowParams());
        mProgressDialogHolder = new ProgressDialogHolder(this);

        mSmartLockViewModel = ViewModelProviders.of(this).get(SmartLockViewModel.class);
        mSmartLockViewModel.getPendingResolution().observe(this,
                new Observer<PendingResolution>() {
                    @Override
                    public void onChanged(@Nullable PendingResolution resolution) {
                        if (resolution == null) {
                            return;
                        }

                        onPendingResolution(resolution);
                    }
                });

        mSmartLockViewModel.getSaveOperation().observe(this,
                new Observer<Resource<Void>>() {
                    @Override
                    public void onChanged(@Nullable Resource<Void> resource) {
                        if (resource == null) {
                            return;
                        }

                        onSaveOperation(resource);
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ExtraConstants.EXTRA_IDP_RESPONSE, mPendingIdpResponse);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgressDialogHolder.dismissDialog();
    }

    public FlowHolder getFlowHolder() {
        if (mFlowHolder == null) {
            mFlowHolder = ViewModelProviders.of(this).get(FlowHolder.class);
            mFlowHolder.init(FlowParameters.fromIntent(getIntent()));
        }

        return mFlowHolder;
    }

    public FlowParameters getFlowParams() {
        if (mFlowParameters == null) {
            mFlowParameters = FlowParameters.fromIntent(getIntent());
        }

        return mFlowParameters;
    }

    public AuthHelper getAuthHelper() {
        return mAuthHelper;
    }

    public ProgressDialogHolder getDialogHolder() {
        return mProgressDialogHolder;
    }

    public void finish(int resultCode, Intent intent) {
        setResult(resultCode, intent);
        finish();
    }

    public void saveCredentialsOrFinish(
            FirebaseUser firebaseUser,
            @Nullable String password,
            IdpResponse response) {
        mPendingIdpResponse = response;

        String accountType = ProviderUtils.idpResponseToAccountType(response);
        mSmartLockViewModel.saveCredentials(firebaseUser, password, accountType);
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    public SmartLockViewModel getSmartLockViewModel() {
        return mSmartLockViewModel;
    }

    private void onPendingResolution(@NonNull PendingResolution resolution) {
        try {
            startIntentSenderForResult(
                    resolution.getPendingIntent().getIntentSender(),
                    resolution.getRequestCode(),
                    null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "STATUS: Failed to send resolution.", e);
            finish(RESULT_OK, mPendingIdpResponse.toIntent());
        };
    }

    private void onSaveOperation(@NonNull Resource<Void> resource) {
        switch (resource.getState()) {
            case LOADING:
                // No-op?
                break;
            case SUCCESS:
            case FAILURE:
                finish(RESULT_OK, mPendingIdpResponse.toIntent());
                break;
        }
    }

}
