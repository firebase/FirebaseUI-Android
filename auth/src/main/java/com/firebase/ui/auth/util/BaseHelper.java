package com.firebase.ui.auth.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.util.smartlock.SaveSmartLock;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static android.app.Activity.RESULT_OK;
import static com.firebase.ui.auth.util.Preconditions.checkNotNull;

public class BaseHelper {
    protected Context mContext;
    private final FlowParameters mFlowParams;
    private ProgressDialog mProgressDialog;

    public BaseHelper(Context context, FlowParameters parameters) {
        mContext = context;
        mFlowParams = parameters;
    }

    public void configureTheme() {
        mContext.setTheme(mFlowParams.themeId);
    }

    public FlowParameters getFlowParams() {
        return mFlowParams;
    }

    public void showLoadingDialog(String message) {
        dismissDialog();
        mProgressDialog = ProgressDialog.show(mContext, "", message, true);
    }

    public void showLoadingDialog(@StringRes int stringResource) {
        showLoadingDialog(mContext.getString(stringResource));
    }

    public void dismissDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    public Context getApplicationContext() {
        return mContext.getApplicationContext();
    }

    public String getAppName() {
        return mFlowParams.appName;
    }

    public FirebaseApp getFirebaseApp() {
        return FirebaseApp.getInstance(mFlowParams.appName);
    }

    public FirebaseAuth getFirebaseAuth() {
        return FirebaseAuth.getInstance(getFirebaseApp());
    }

    public CredentialsApi getCredentialsApi() {
        return Auth.CredentialsApi;
    }

    public FirebaseUser getCurrentUser() {
        return getFirebaseAuth().getCurrentUser();
    }

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

    public void saveCredentialsOrFinish(
            @Nullable SaveSmartLock saveSmartLock,
            AppCompatBase activity,
            FirebaseUser firebaseUser,
            @NonNull IdpResponse response) {
        saveCredentialsOrFinish(saveSmartLock, activity, firebaseUser, null, response);
    }

    public void saveCredentialsOrFinish(
            @Nullable SaveSmartLock saveSmartLock,
            AppCompatBase activity,
            FirebaseUser firebaseUser,
            @Nullable String password,
            @Nullable IdpResponse response) {
        if (saveSmartLock == null) {
            activity.finish(RESULT_OK, new Intent());
        } else {
            saveSmartLock.saveCredentialsOrFinish(
                    activity,
                    firebaseUser,
                    password,
                    response);
        }
    }
}
