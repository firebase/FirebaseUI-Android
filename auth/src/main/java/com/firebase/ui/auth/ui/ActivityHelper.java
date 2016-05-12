/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.StringRes;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class ActivityHelper {
    private ProgressDialog mProgressDialog;
    private Activity mActivity;

    public String appName;
    public ArrayList<IDPProviderParcel> providerParcels;
    public String termsOfServiceUrl;
    public int theme;


    public ActivityHelper(Activity activity, Intent intent) {
        mActivity = activity;
        appName = intent.getStringExtra(ControllerConstants.EXTRA_APP_NAME);
        providerParcels = intent.getParcelableArrayListExtra(
                ControllerConstants.EXTRA_PROVIDERS);
        termsOfServiceUrl = intent.getStringExtra(
                ControllerConstants.EXTRA_TERMS_OF_SERVICE_URL);
        theme = intent.getIntExtra(
                ControllerConstants.EXTRA_THEME,
                AuthUI.DEFAULT_THEME);
    }

    public void dismissDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    public void showLoadingDialog(String message) {
        dismissDialog();
        mProgressDialog = ProgressDialog.show(mActivity, "", message, true);
    }

    public void showLoadingDialog(@StringRes int stringResource) {
        showLoadingDialog(mActivity.getString(stringResource));
    }


    private Intent addExtras(Intent intent) {
        intent.putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
                .putExtra(ControllerConstants.EXTRA_TERMS_OF_SERVICE_URL, termsOfServiceUrl)
                .putExtra(ControllerConstants.EXTRA_THEME, theme)
                .putExtra(ControllerConstants.EXTRA_PROVIDERS, providerParcels);
        return intent;
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        mActivity.startActivityForResult(intent, requestCode);
    }

    public void finish(int resultCode, Intent intent) {
        mActivity.setResult(resultCode, addExtras(intent));
        mActivity.finish();
    }

    public Context getApplicationContext() {
        return mActivity.getApplicationContext();
    }


    public FirebaseApp getFirebaseApp() {
        return FirebaseApp.getInstance(appName);
    }

    public FirebaseApp getFirebaseApp(String apiaryKey, String applicationId) {
        try{
            return FirebaseApp.getInstance(appName);
        } catch (IllegalStateException e) {
            FirebaseOptions options
                    = new FirebaseOptions.Builder()
                    .setApiKey(apiaryKey)
                    .setApplicationId(applicationId)
                    .build();
            return FirebaseApp.initializeApp(mActivity, options, appName);
        }
    }

    public FirebaseAuth getFirebaseAuth() {
        return FirebaseAuth.getInstance(getFirebaseApp());
    }

    public FirebaseUser getCurrentUser() {
        return getFirebaseAuth().getCurrentUser();
    }

}
