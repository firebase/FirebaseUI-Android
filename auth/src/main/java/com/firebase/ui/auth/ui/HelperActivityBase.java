package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v7.app.AppCompatActivity;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.credentials.CredentialSaveActivity;
import com.firebase.ui.auth.util.AuthHelper;
import com.firebase.ui.auth.util.CredentialsUtil;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.FlowHolder;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.firebase.auth.FirebaseUser;

import static com.firebase.ui.auth.util.Preconditions.checkNotNull;

@SuppressWarnings("Registered")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HelperActivityBase extends AppCompatActivity {

    private static final String TAG = "HelperActivityBase";

    private static final int RC_SAVE_CREDENTIAL = 101;

    private FlowHolder mFlowHolder;

    private FlowParameters mFlowParameters;
    private AuthHelper mAuthHelper;
    private ProgressDialogHolder mProgressDialogHolder;

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

        // Forward the results of Smartlock Saving
        if (requestCode == RC_SAVE_CREDENTIAL) {
            finish(RESULT_OK, data);
        }
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

    public void startSaveCredentials(
            FirebaseUser firebaseUser,
            @Nullable String password,
            IdpResponse response) {

        // Build credential
        String accountType = ProviderUtils.idpResponseToAccountType(response);
        Credential credential = CredentialsUtil.buildCredential(
                firebaseUser, password, accountType);

        // Start the dedicated SmartLock Activity
        Intent intent = CredentialSaveActivity.createIntent(this, getFlowParams(),
                credential, response);
        startActivityForResult(intent, RC_SAVE_CREDENTIAL);
    }
}
