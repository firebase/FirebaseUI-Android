package com.firebase.ui.auth.ui.credentials;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.ui.InvisibleActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.firebase.ui.auth.viewmodel.smartlock.SmartLockHandler;
import com.google.android.gms.auth.api.credentials.Credential;

/**
 * Invisible Activity used for saving credentials to SmartLock.
 */
public class CredentialSaveActivity extends InvisibleActivityBase {
    private static final String TAG = "CredentialSaveActivity";

    private SmartLockHandler mHandler;

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

        final IdpResponse response = getIntent().getParcelableExtra(ExtraConstants.IDP_RESPONSE);
        Credential credential = getIntent().getParcelableExtra(ExtraConstants.CREDENTIAL);

        mHandler = ViewModelProviders.of(this).get(SmartLockHandler.class);
        mHandler.init(getFlowParams());
        mHandler.setResponse(response);

        mHandler.getOperation().observe(this, new ResourceObserver<IdpResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdpResponse response) {
                finish(RESULT_OK, response.toIntent());
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                // RESULT_OK since we don't want to halt sign-in just because of a credential save
                // error.
                finish(RESULT_OK, response.toIntent());
            }
        });

        // Avoid double-saving
        Resource<IdpResponse> currentOp = mHandler.getOperation().getValue();
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
