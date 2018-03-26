package com.firebase.ui.auth.ui.credentials;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.viewmodel.VoidResourceObserver;
import com.firebase.ui.auth.viewmodel.smartlock.SmartLockHandler;
import com.google.android.gms.auth.api.credentials.Credential;

/**
 * Invisible Activity used for saving credentials to SmartLock.
 */
public class CredentialSaveActivity extends HelperActivityBase {
    private static final String TAG = "CredentialSaveActivity";

    private SmartLockHandler mHandler;
    private IdpResponse mIdpResponse;

    @NonNull
    public static Intent createIntent(Context context,
                                      FlowParameters flowParams,
                                      Credential credential,
                                      IdpResponse response) {
        return createBaseIntent(context, CredentialSaveActivity.class, flowParams)
                .putExtra(ExtraConstants.CREDENTIAL, credential)
                .putExtra(ExtraConstants.IDP_RESPONSE, response);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = ViewModelProviders.of(this).get(SmartLockHandler.class);
        mHandler.init(getFlowParams());

        Credential credential = getIntent().getParcelableExtra(ExtraConstants.CREDENTIAL);
        mIdpResponse = getIntent().getParcelableExtra(ExtraConstants.IDP_RESPONSE);

        mHandler.getOperation().observe(this, new VoidResourceObserver(
                this, R.string.fui_progress_dialog_loading) {
            @Override
            protected void onSuccess() {
                finish(RESULT_OK, mIdpResponse.toIntent());
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                // RESULT_OK since we don't want to halt sign-in just because of a credential save
                // error.
                finish(RESULT_OK, mIdpResponse.toIntent());
            }
        });

        // Avoid double-saving
        Resource<Void> currentOp = mHandler.getOperation().getValue();
        if (currentOp == null) {
            Log.d(TAG, "Launching save operation.");
            mHandler.saveCredentials(credential);
        } else {
            Log.d(TAG, "Save operation in progress, doing nothing.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mHandler.onActivityResult(requestCode, resultCode);
    }
}
