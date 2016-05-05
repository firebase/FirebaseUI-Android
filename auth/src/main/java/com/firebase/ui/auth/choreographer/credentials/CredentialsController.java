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

package com.firebase.ui.auth.choreographer.credentials;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.firebase.ui.auth.api.CredentialsAPI;
import com.firebase.ui.auth.api.FirebaseAuthWrapperFactory;
import com.firebase.ui.auth.choreographer.Action;
import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.Result;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.firebase.ui.auth.ui.credentials.ChooseAccountActivity;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.firebase.ui.auth.ui.idp.IDPSignInContainerActivity;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;

public class CredentialsController implements Controller {
    private static final String TAG = "CredentialsController";
    private Context mContext;
    private CredentialsAPI mCredentialsApi;

    // states
    public static final int ID_INIT = 10;
    public static final int ID_CHOOSE_ACCOUNT = 20;
    private String mAppName;

    public CredentialsController(Context context, CredentialsAPI credentialsApi, String appName) {
        mCredentialsApi = credentialsApi;
        mContext = context;
        mAppName = appName;
    }

    @Override
    public Action next(Result result) {
        Intent data = result.getData();

        String email = mCredentialsApi.getEmailFromCredential();
        String password = mCredentialsApi.getPasswordFromCredential();
        String accountType = mCredentialsApi.getAccountTypeFromCredential();

        ArrayList<IDPProviderParcel> providers =
                data.getParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS);

        switch (result.getId()) {
            case ID_INIT:
                if (mCredentialsApi.isPlayServicesAvailable()
                        && mCredentialsApi.isCredentialsAvailable()) {
                    if (mCredentialsApi.isAutoSignInAvailable()) {
                        mCredentialsApi.googleSilentSignIn();
                        // TODO: (serikb) authenticate Firebase user and continue to application
                        FirebaseUser loggedInUser;
                        if (password != null && !password.isEmpty()) {
                            loggedInUser =
                                    FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(mAppName)
                                            .signInWithEmailPassword(email, password);
                            return finishAction(Activity.RESULT_OK, loggedInUser);
                        } else {
                            return redirectToIdpSignIn(email, accountType, providers);
                        }
                    } else if (mCredentialsApi.isSignInResolutionNeeded()) {
                        return Action.next(
                                ID_CHOOSE_ACCOUNT,
                                new Intent(mContext, ChooseAccountActivity.class)
                                        .putParcelableArrayListExtra(
                                                ControllerConstants.EXTRA_PROVIDERS,
                                                providers));
                    }
                }
                break;
            case ID_CHOOSE_ACCOUNT:
                Log.e(TAG, "email: " + email);
                if (email != null
                        && mCredentialsApi.isCredentialsAvailable()
                        && !mCredentialsApi.isSignInResolutionNeeded()) {
                    // TODO: (serikb) authenticate Firebase user and continue to application
                    FirebaseUser loggedInUser;
                    if (password != null && !password.isEmpty()) {
                        loggedInUser =
                                FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(mAppName)
                                        .signInWithEmailPassword(email, password);
                        return finishAction(Activity.RESULT_OK, loggedInUser);
                    } else {
                        return redirectToIdpSignIn(email, accountType, providers);
                    }
                }
                break;
            default:
                Log.w(TAG, "No handler defined for state " + result.getId());
        }

        return Action.startFlow(
                AuthMethodPickerActivity.createIntent(
                        mContext,
                        mAppName,
                        providers));
    }

    private Action redirectToIdpSignIn(
            String email, String accountType, ArrayList<IDPProviderParcel> providers) {
        switch (accountType) {
            case IdentityProviders.GOOGLE:
                return Action.startFlow(
                        IDPSignInContainerActivity.createIntent(
                                mContext,
                                GoogleAuthProvider.PROVIDER_ID,
                                email,
                                providers,
                                mAppName));
            case IdentityProviders.FACEBOOK:
                return Action.startFlow(
                        IDPSignInContainerActivity.createIntent(
                                mContext,
                                FacebookAuthProvider.PROVIDER_ID,
                                email,
                                providers,
                                mAppName));
            default:
                Log.w(TAG, "unknown provider: " + accountType);
                return Action.startFlow(
                        AuthMethodPickerActivity.createIntent(mContext, mAppName, providers));
        }
    }

    private Action finishAction(int resultCode, FirebaseUser firebaseUser) {
        Bundle finishingData = new Bundle();

        if (firebaseUser != null) {
            finishingData.putString(ControllerConstants.EXTRA_EMAIL, firebaseUser.getEmail());
        }

        Intent finishingIntent = new Intent();

        finishingIntent.putExtra(ControllerConstants.EXTRA_EMAIL,
                finishingData.getString(ControllerConstants.EXTRA_EMAIL));

        return Action.finish(resultCode, finishingIntent);
    }
}
