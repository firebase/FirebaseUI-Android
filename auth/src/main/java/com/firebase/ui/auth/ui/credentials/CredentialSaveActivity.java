package com.firebase.ui.auth.ui.credentials;

import android.arch.lifecycle.Observer;
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
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.ui.FlowUtils;
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
        return HelperActivityBase.createBaseIntent(context, CredentialSaveActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_CREDENTIAL, credential)
                .putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, response);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = ViewModelProviders.of(this).get(SmartLockHandler.class);
        mHandler.init(getFlowParams());

        Credential credential = getIntent().getParcelableExtra(ExtraConstants.EXTRA_CREDENTIAL);
        mIdpResponse = getIntent().getParcelableExtra(ExtraConstants.EXTRA_IDP_RESPONSE);

        mHandler.getOperation().observe(this, new Observer<Resource<Void>>() {
            @Override
            public void onChanged(@Nullable Resource<Void> resource) {
                if (resource == null) {
                    Log.w(TAG, "getSaveOperation:onChanged:null");
                    return;
                }

                onSaveOperation(resource);
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

    private void onSaveOperation(@NonNull Resource<Void> resource) {
        switch (resource.getState()) {
            case LOADING:
                // No-op?
                break;
            case SUCCESS:
            case FAILURE:
                if (!resource.isUsed()
                        && !FlowUtils.handleError(this, resource.getException())) {
                    finish(RESULT_OK, mIdpResponse.toIntent());
                }
                break;
        }
    }
}
