package com.firebase.ui.auth.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public abstract class NoControllerBaseActivity extends android.support.v7.app.AppCompatActivity {
    protected String mAppName;
    protected ArrayList<IDPProviderParcel> mProviderParcels;
    protected String mTermsOfServiceUrl;

    // TODO once the controller-centric BaseActivity is gone this will no longer be duplicate code
    private ProgressDialog mProgressDialog;
    private void dismissDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    protected void showLoadingDialog(String message) {
        dismissDialog();
        mProgressDialog = ProgressDialog.show(this, "", message, true);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        mAppName = getIntent().getStringExtra(ControllerConstants.EXTRA_APP_NAME);
        mProviderParcels = getIntent().getParcelableArrayListExtra(
                ControllerConstants.EXTRA_PROVIDERS);
        mTermsOfServiceUrl = getIntent().getStringExtra(
                ControllerConstants.EXTRA_TERMS_OF_SERVICE_URL);
    }

    private Intent addExtras(Intent intent) {
        intent.putExtra(ControllerConstants.EXTRA_APP_NAME, mAppName)
                .putExtra(ControllerConstants.EXTRA_TERMS_OF_SERVICE_URL, mTermsOfServiceUrl)
                .putExtra(ControllerConstants.EXTRA_PROVIDERS, mProviderParcels);
        return intent;
    }

    public void finish(int resultCode, Intent intent) {
        setResult(resultCode, addExtras(intent));
        finish();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(addExtras(intent));
    }

    protected FirebaseApp getFirebaseApp() {
         return FirebaseApp.getInstance(mAppName);
    }

    protected FirebaseAuth getFirebaseAuth() {
        return FirebaseAuth.getInstance(getFirebaseApp());
    }

    protected FirebaseUser getCurrentUser() {
        return getFirebaseAuth().getCurrentUser();
    }

}
