package com.firebase.ui.auth;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.data.remote.SignInKickstarter;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.PlayServicesHelper;
import com.firebase.ui.auth.util.ui.FlowUtils;
import com.firebase.ui.auth.viewmodel.RequestCodes;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class KickoffActivity extends HelperActivityBase {
    private static final String IS_WAITING_FOR_PLAY_SERVICES = "is_waiting_for_play_services";

    private SignInKickstarter mKickstarter;
    private boolean mIsWaitingForPlayServices = false;

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return HelperActivityBase.createBaseIntent(context, KickoffActivity.class, flowParams);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mKickstarter = ViewModelProviders.of(this).get(SignInKickstarter.class);
        mKickstarter.init(getFlowParams());
        mKickstarter.getOperation().observe(this, new Observer<Resource<IdpResponse>>() {
            @Override
            public void onChanged(Resource<IdpResponse> resource) {
                if (resource.getState() == State.LOADING) {
                    getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_loading);
                    return;
                }
                getDialogHolder().dismissDialog();

                if (resource.isUsed()) { return; }

                if (resource.getState() == State.SUCCESS) {
                    finish(RESULT_OK, resource.getValue().toIntent());
                } else {
                    Exception e = resource.getException();
                    if (!FlowUtils.handleError(KickoffActivity.this, e)) {
                        finish(RESULT_CANCELED, IdpResponse.getErrorIntent(e));
                    }
                }
            }
        });

        if (savedInstanceState == null || savedInstanceState.getBoolean(IS_WAITING_FOR_PLAY_SERVICES)) {
            init();
        }
    }

    private void init() {
        if (isOffline()) {
            finish(RESULT_CANCELED, IdpResponse.getErrorIntent(
                    new FirebaseUiException(ErrorCodes.NO_NETWORK)));
            return;
        }

        boolean isPlayServicesAvailable = PlayServicesHelper.makePlayServicesAvailable(
                this,
                RequestCodes.PLAY_SERVICES_CHECK,
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish(RESULT_CANCELED, IdpResponse.getErrorIntent(
                                new FirebaseUiException(ErrorCodes.PLAY_SERVICES_UPDATE_CANCELLED)));
                    }
                });

        if (isPlayServicesAvailable) {
            mKickstarter.start();
        } else {
            mIsWaitingForPlayServices = true;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // It doesn't matter what we put here, we just don't want outState to be empty
        outState.putBoolean(ExtraConstants.HAS_EXISTING_INSTANCE, true);
        outState.putBoolean(IS_WAITING_FOR_PLAY_SERVICES, mIsWaitingForPlayServices);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.PLAY_SERVICES_CHECK) {
            if (resultCode == RESULT_OK) {
                mKickstarter.start();
            } else {
                finish(RESULT_CANCELED, IdpResponse.getErrorIntent(
                        new FirebaseUiException(ErrorCodes.PLAY_SERVICES_UPDATE_CANCELLED)));
            }
        } else {
            mKickstarter.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Check if there is an active or soon-to-be-active network connection.
     *
     * @return true if there is no network connection, false otherwise.
     */
    private boolean isOffline() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        return !(manager != null
                && manager.getActiveNetworkInfo() != null
                && manager.getActiveNetworkInfo().isConnectedOrConnecting());
    }
}
