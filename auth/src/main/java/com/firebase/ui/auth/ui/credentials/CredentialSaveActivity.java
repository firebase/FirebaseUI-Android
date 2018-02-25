package com.firebase.ui.auth.ui.credentials;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.viewmodel.PendingResolution;
import com.firebase.ui.auth.viewmodel.ResolutionCodes;
import com.firebase.ui.auth.viewmodel.smartlock.SmartLockHandler;
import com.google.android.gms.auth.api.credentials.Credential;

/**
 * Invisible Activity used for saving credentials to SmartLock.
 */
public class CredentialSaveActivity extends HelperActivityBase {

    private static final String TAG = "SmartlockSave";

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

        mHandler.getSaveOperation().observe(this, new Observer<Resource<Void>>() {
            @Override
            public void onChanged(@Nullable Resource<Void> resource) {
                if (resource == null) {
                    Log.w(TAG, "getSaveOperation:onChanged:null");
                    return;
                }

                onSaveOperation(resource);
            }
        });

        mHandler.getPendingResolution().observe(this, new Observer<PendingResolution>() {
            @Override
            public void onChanged(@Nullable PendingResolution resolution) {
                if (resolution == null) {
                    Log.w(TAG, "getPendingResolution:onChanged: null");
                    return;
                }

                onPendingResolution(resolution);
            }
        });

        // Avoid double-saving
        Resource<Void> currentOp = mHandler.getSaveOperation().getValue();
        if (currentOp == null) {
            Log.d(TAG, "Launching save operation.");
            mHandler.saveCredentials(credential);
        } else {
            Log.d(TAG, "Save operation in progress, doing nothing.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Forward activity results to the ViewModel
        if (!mHandler.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onSaveOperation(@NonNull Resource<Void> resource) {
        switch (resource.getState()) {
            case LOADING:
                // No-op?
                break;
            case SUCCESS:
            case FAILURE:
                finish(RESULT_OK, mIdpResponse.toIntent());
                break;
        }
    }

    private void onPendingResolution(@NonNull PendingResolution resolution) {
        if (resolution.getRequestCode() == ResolutionCodes.RC_CRED_SAVE) {
            try {
                startIntentSenderForResult(
                        resolution.getPendingIntent().getIntentSender(),
                        resolution.getRequestCode(),
                        null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Failed to send resolution.", e);
                finish(RESULT_OK, mIdpResponse.toIntent());
            };
        }
    }

}
