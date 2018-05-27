package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.credentials.CredentialSaveActivity;
import com.firebase.ui.auth.util.AuthHelper;
import com.firebase.ui.auth.util.CredentialUtils;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.firebase.auth.FirebaseUser;

import static com.firebase.ui.auth.util.Preconditions.checkNotNull;

@SuppressWarnings("Registered")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HelperActivityBase extends AppCompatActivity implements ProgressView {
    private FlowParameters mParams;

    private AuthHelper mAuthHelper;
    private ProgressDialogHolder mProgressDialogHolder;

    protected static Intent createBaseIntent(
            @NonNull Context context,
            @NonNull Class<? extends Activity> target,
            @NonNull FlowParameters flowParams) {
        return new Intent(
                checkNotNull(context, "context cannot be null"),
                checkNotNull(target, "target activity cannot be null"))
                .putExtra(ExtraConstants.FLOW_PARAMS,
                        checkNotNull(flowParams, "flowParams cannot be null"));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuthHelper = new AuthHelper(getFlowParams());
        mProgressDialogHolder = new ProgressDialogHolder(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgressDialogHolder.dismissDialog();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Forward the results of Smart Lock saving
        if (requestCode == RequestCodes.CRED_SAVE_FLOW) {
            finish(resultCode, data);
        }
    }

    public FlowParameters getFlowParams() {
        if (mParams == null) {
            mParams = FlowParameters.fromIntent(getIntent());
        }
        return mParams;
    }

    public AuthHelper getAuthHelper() {
        return mAuthHelper;
    }

    public void finish(int resultCode, @Nullable Intent intent) {
        setResult(resultCode, intent);
        finish();
    }

    public void startSaveCredentials(
            FirebaseUser firebaseUser,
            IdpResponse response,
            @Nullable String password) {
        // Build credential
        String accountType = ProviderUtils.idpResponseToAccountType(response);
        Credential credential = CredentialUtils.buildCredential(
                firebaseUser, password, accountType);

        // Start the dedicated SmartLock Activity
        Intent intent = CredentialSaveActivity.createIntent(
                this, getFlowParams(), credential, response);
        startActivityForResult(intent, RequestCodes.CRED_SAVE_FLOW);
    }

    @Override
    public void showProgress(@StringRes int message) {
        getDialogHolder().showLoadingDialog(message);
    }

    @Override
    public void hideProgress() {
        getDialogHolder().dismissDialog();
    }

    protected ProgressDialogHolder getDialogHolder() {
        return mProgressDialogHolder;
    }
}
