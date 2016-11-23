package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
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

    public void finishActivity(Activity activity, int resultCode, Intent intent) {
        activity.setResult(resultCode, intent);
        activity.finish();
    }

    public void showLoadingDialog(String message) {
        dismissDialog();
        mProgressDialog = ProgressDialog.show(mContext, "", message, true);
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

    public SaveSmartLock getSaveSmartLockInstance(FragmentActivity activity) {
        return SaveSmartLock.getInstance(activity, getFlowParams());
    }

    public void saveCredentialsOrFinish(
            @Nullable SaveSmartLock saveSmartLock,
            Activity activity,
            FirebaseUser firebaseUser,
            @NonNull IdpResponse response) {
        saveCredentialsOrFinish(saveSmartLock, activity, firebaseUser, null, response);
    }

    public void saveCredentialsOrFinish(
            @Nullable SaveSmartLock saveSmartLock,
            Activity activity,
            FirebaseUser firebaseUser,
            @Nullable String password,
            @Nullable IdpResponse response) {
        if (saveSmartLock == null) {
            finishActivity(activity, RESULT_OK, new Intent());
        } else {
            saveSmartLock.saveCredentialsOrFinish(
                    firebaseUser,
                    password,
                    response);
        }
    }
}
