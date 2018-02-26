package com.firebase.ui.auth;

import android.app.PendingIntent;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.RestrictTo;
import android.util.Pair;

import com.firebase.ui.auth.data.model.FirebaseUiException;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.data.remote.SignInKickstarter;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.PlayServicesHelper;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class KickoffActivity extends HelperActivityBase {
    private static final String IS_WAITING_FOR_PLAY_SERVICES = "is_waiting_for_play_services";
    private static final int RC_PLAY_SERVICES = 1;

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

                if (resource.getState() == State.SUCCESS) {
                    finish(RESULT_OK, resource.getValue().toIntent());
                } else {
                    if (resource.getException() instanceof UserCancellationException) {
                        finish(RESULT_CANCELED, null);
                    } else {
                        finish(RESULT_CANCELED,
                                IdpResponse.fromError(resource.getException()).toIntent());
                    }
                }
            }
        });
        mKickstarter.getIntentReqester().observe(this, new Observer<Pair<Intent, Integer>>() {
            @Override
            public void onChanged(Pair<Intent, Integer> pair) {
                getDialogHolder().dismissDialog();
                startActivityForResult(pair.first, pair.second);
            }
        });
        mKickstarter.getPendingIntentReqester()
                .observe(this, new Observer<Pair<PendingIntent, Integer>>() {
                    @Override
                    public void onChanged(Pair<PendingIntent, Integer> pair) {
                        getDialogHolder().dismissDialog();
                        try {
                            startIntentSenderForResult(
                                    pair.first.getIntentSender(), pair.second, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            finish(RESULT_CANCELED, IdpResponse.fromError(e).toIntent());
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
                RC_PLAY_SERVICES,
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
        if (requestCode == RC_PLAY_SERVICES) {
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
