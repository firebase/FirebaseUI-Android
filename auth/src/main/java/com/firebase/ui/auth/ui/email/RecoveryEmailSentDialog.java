package com.firebase.ui.auth.ui.email;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.BaseDialog;
import com.firebase.ui.auth.ui.ExtraConstants;

public class RecoveryEmailSentDialog extends BaseDialog {
    private static final String TAG = "RecoveryEmailSentDialog";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext(), R.style.FirebaseUI_Dialog)
                .setTitle(R.string.title_confirm_recover_password_activity)
                .setMessage(String.format(getString(R.string.confirm_recovery_body),
                                          getArguments().getString(ExtraConstants.EXTRA_EMAIL)))
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface anInterface) {
                        finish(Activity.RESULT_OK, new Intent());
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public static void show(String email, FragmentManager manager) {
        RecoveryEmailSentDialog result = new RecoveryEmailSentDialog();
        Bundle bundle = new Bundle();
        bundle.putString(ExtraConstants.EXTRA_EMAIL, email);
        result.setArguments(bundle);
        result.show(manager, TAG);
    }
}
