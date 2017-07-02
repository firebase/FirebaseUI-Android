package com.firebase.ui.auth.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.StringRes;

/**
 * Helper class to manage a ProgressDialog.
 */
public class ProgressDialogHolder {

    private Context mContext;
    private ProgressDialog mProgressDialog;

    public ProgressDialogHolder(Context context) {
        mContext = context;
    }

    private void showLoadingDialog(String message) {
        dismissDialog();

        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setTitle("");
        }

        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    public void showLoadingDialog(@StringRes int stringResource) {
        showLoadingDialog(mContext.getString(stringResource));
    }

    public void dismissDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    public boolean isProgressDialogShowing() {
        return mProgressDialog != null && mProgressDialog.isShowing();
    }

}
