package com.firebase.ui.auth.ui.email;

import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.firebase.ui.auth.viewmodel.email.EmailLinkSignInHandler;

public class EmailLinkCatcherActivity extends AppCompatBase {

    private EmailLinkSignInHandler mHandler;

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return createBaseIntent(context, EmailLinkCatcherActivity.class, flowParams);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initHandler(buildAlertDialogForWrongDevice());

        if (getFlowParams().emailLink != null) {
            mHandler.startSignIn();
        }
    }

    private void initHandler(final AlertDialog alertDialog) {
        mHandler = ViewModelProviders.of(this).get(EmailLinkSignInHandler.class);
        mHandler.init(getFlowParams());
        mHandler.getOperation().observe(this, new ResourceObserver<IdpResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdpResponse response) {
                finish(RESULT_OK, response.toIntent());
            }

            @Override
            protected void onFailure(@NonNull final Exception e) {
                if (e instanceof UserCancellationException) {
                    finish(RESULT_CANCELED, null);
                } else if (e instanceof FirebaseAuthAnonymousUpgradeException) {
                    IdpResponse res = ((FirebaseAuthAnonymousUpgradeException) e).getResponse();
                    finish(RESULT_CANCELED, new Intent().putExtra(ExtraConstants
                            .IDP_RESPONSE, res));
                } else {
                    if (e instanceof FirebaseUiException && ((FirebaseUiException) e)
                            .getErrorCode() == ErrorCodes.EMAIL_LINK_WRONG_DEVICE_ERROR) {
                        alertDialog.show();
                    } else {
                        finish(RESULT_CANCELED, IdpResponse.getErrorIntent(e));
                    }
                }
            }
        });
    }

   private AlertDialog buildAlertDialogForWrongDevice() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(R.string.fui_email_link_wrong_device_header)
                .setMessage(R.string.fui_email_link_wrong_device_message)
                .setPositiveButton(R.string
                        .fui_email_link_wrong_device_dismiss_button, new
                        DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish(RequestCodes.EMAIL_LINK_WRONG_DEVICE_FLOW, null);
                            }
                        });
        return alertDialog.create();
    }

    @Override
    public void showProgress(int message) {
        // Do nothing
    }

    @Override
    public void hideProgress() {
        // Do nothing
    }

}
