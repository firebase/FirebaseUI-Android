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

package com.firebase.ui.auth.choreographer.email;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.api.FirebaseAuthWrapperFactory;
import com.firebase.ui.auth.api.FirebaseAuthWrapper;
import com.firebase.ui.auth.choreographer.Action;
import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.Result;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.firebase.ui.auth.ui.account_link.WelcomeBackIDPPrompt;
import com.firebase.ui.auth.ui.email.ConfirmRecoverPasswordActivity;
import com.firebase.ui.auth.ui.email.EmailFlowBaseActivity;
import com.firebase.ui.auth.ui.email.RecoverPasswordActivity;
import com.firebase.ui.auth.ui.email.RegisterEmailActivity;
import com.firebase.ui.auth.ui.email.SignInActivity;
import com.firebase.ui.auth.ui.email.SignInNoPasswordActivity;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;


public class EmailFlowController implements Controller {

    static final String TAG = "AddEmailController";

    public static final int ID_SELECT_EMAIL = 10;
    public static final int ID_SIGN_IN = 20;
    static final int ID_REGISTER_EMAIL = 40;
    public static final int ID_RECOVER_PASSWORD = 80;
    static final int ID_CONFIRM_RECOVER_PASSWORD = 90;
    private final Context mAppContext;
    private final String mAppName;

    @Override
    public Action next(Result result) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Perform next action based on result with id: " + result.getResultCode());
        }

        // Additional local variables
        Intent data = result.getData();
        String email, password;
        boolean restorePassword;

        email = data.getStringExtra(ControllerConstants.EXTRA_EMAIL);
        password = data.getStringExtra(ControllerConstants.EXTRA_PASSWORD);
        restorePassword = data.getBooleanExtra(ControllerConstants.EXTRA_RESTORE_PASSWORD_FLAG,
                false);
        FirebaseAuthWrapper apiWrapper = FirebaseAuthWrapperFactory.getFirebaseAuthWrapper
                (mAppName);

        switch (result.getId()) {
            case ID_SELECT_EMAIL:
                if (result.getResultCode() == EmailFlowBaseActivity.BACK_IN_FLOW) {
                    return Action.finish(Activity.RESULT_CANCELED, null);
                }
                //When the email input activity exits incorrectly, show the SignInNoPasswordActivity
                if(result.getResultCode() == Activity.RESULT_CANCELED) {
                    return Action.next(ID_SELECT_EMAIL,
                            new Intent(mAppContext, SignInNoPasswordActivity.class));
                }
                boolean isAccountExists = false;
                if (email != null && !email.isEmpty()) {
                    isAccountExists = apiWrapper.isExistingAccount(email);
                }
                if(!isAccountExists) {
                    Intent registerIntent = new Intent(mAppContext, RegisterEmailActivity.class);
                    registerIntent.putExtra(ControllerConstants.EXTRA_EMAIL, email);
                    return Action.next(ID_REGISTER_EMAIL, registerIntent);
                }
                List<String> providers =
                        apiWrapper.getProvidersForEmail(email);

                for (String provider: providers) {
                    if (provider.equalsIgnoreCase(EmailAuthProvider.PROVIDER_ID)) {
                        Intent signInIntent = new Intent(mAppContext, SignInActivity.class);
                        signInIntent.putExtra(ControllerConstants.EXTRA_EMAIL, email);
                        return Action.next(ID_SIGN_IN, signInIntent);
                    }
                    return Action.startFlow(WelcomeBackIDPPrompt.createIntent(
                            mAppContext,
                            provider,
                            mAppName,
                            email));
                }

                Intent signInIntent = new Intent(mAppContext, SignInActivity.class);
                signInIntent.putExtra(ControllerConstants.EXTRA_EMAIL, email);
                return Action.next(ID_SIGN_IN, signInIntent);

            case ID_SIGN_IN:
                if (result.getResultCode() == EmailFlowBaseActivity.BACK_IN_FLOW) {
                    return Action.finish(Activity.RESULT_CANCELED, null);
                }
                if (restorePassword) {
                    Intent recoverIntent = new Intent(mAppContext, RecoverPasswordActivity.class);
                    recoverIntent.putExtra(ControllerConstants.EXTRA_EMAIL, email);
                    return Action.next(ID_RECOVER_PASSWORD, recoverIntent);
                } else {
                    FirebaseUser firebaseUser =
                                    apiWrapper.signInWithEmailPassword(email, password);
                    return handleLoginResult(
                            email,
                            password,
                            firebaseUser,
                            mAppContext.getString(com.firebase.ui.auth.R.string.login_error));
                }
            case ID_REGISTER_EMAIL:
                if (result.getResultCode() == EmailFlowBaseActivity.BACK_IN_FLOW) {
                    Intent intent = new Intent(mAppContext, SignInNoPasswordActivity.class);
                    intent.putExtra(ControllerConstants.EXTRA_EMAIL, email);
                    return Action.back(ID_SELECT_EMAIL, intent);
                }
                FirebaseUser firebaseUser = apiWrapper.createUserWithEmailAndPassword(email, password);
                return handleLoginResult(email, password, firebaseUser, mAppContext.getString(
                                com.firebase.ui.auth.R.string.email_account_creation_error));
            case ID_RECOVER_PASSWORD:
                if (result.getResultCode() == EmailFlowBaseActivity.BACK_IN_FLOW) {
                    signInIntent = new Intent(mAppContext, SignInActivity.class);
                    signInIntent.putExtra(ControllerConstants.EXTRA_EMAIL, email);
                    return Action.back(ID_SIGN_IN, signInIntent);
                }
                boolean isSuccess =
                        apiWrapper.resetPasswordForEmail(email);
                Intent confirmIntent = new Intent(mAppContext,
                        ConfirmRecoverPasswordActivity.class);
                confirmIntent.putExtra(ControllerConstants.EXTRA_SUCCESS, isSuccess);
                confirmIntent.putExtra(ControllerConstants.EXTRA_EMAIL, email);
                    return Action.next(ID_CONFIRM_RECOVER_PASSWORD, confirmIntent);
            case ID_CONFIRM_RECOVER_PASSWORD:
                if (result.getResultCode() == EmailFlowBaseActivity.BACK_IN_FLOW) {
                    Intent recoverIntent = new Intent(mAppContext, RecoverPasswordActivity.class);
                    recoverIntent.putExtra(ControllerConstants.EXTRA_EMAIL, email);
                    return Action.back(ID_RECOVER_PASSWORD, recoverIntent);
                }
                signInIntent = new Intent(mAppContext, SignInActivity.class);
                return Action.next(ID_SIGN_IN, signInIntent);
            default:
                return finish(Activity.RESULT_FIRST_USER, (FirebaseUser) null);
        }
    }

   private Action handleLoginResult(
           String email,
           String password,
           FirebaseUser firebaseUser,
           String errorMsg) {
        Intent data = new Intent();
        data.putExtra(ControllerConstants.EXTRA_ERROR_MESSAGE, errorMsg);
        if (firebaseUser != null) {
            if(FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(mAppName)
                    .isPlayServicesAvailable(mAppContext)) {
                Intent saveCredentialIntent =
                        SaveCredentialsActivity.createIntent(
                                mAppContext,
                                firebaseUser.getDisplayName(),
                                firebaseUser.getEmail(),
                                password,
                                null,
                                null,
                                mAppName
                        );
                return Action.startFlow(saveCredentialIntent);
            }
            return finish(Activity.RESULT_OK, firebaseUser);
        } else {
            return Action.block(data);
        }
    }

    private Action finish(int result_code, FirebaseUser firebaseUser) {
        Bundle finishingData = new Bundle();

        if(firebaseUser != null) {
            finishingData.putString(ControllerConstants.EXTRA_EMAIL, firebaseUser.getEmail());
        }

        Action finalAction = finish(result_code, finishingData);

        return finalAction;
    }

    private Action finish(int result_code, Bundle resultBundle) {
        Intent finishingData = new Intent();

        finishingData.putExtra(ControllerConstants.EXTRA_EMAIL,
                resultBundle.getString(ControllerConstants.EXTRA_EMAIL));
        finishingData.putExtra(ControllerConstants.EXTRA_ID_TOKEN,
                resultBundle.getString(ControllerConstants.EXTRA_ID_TOKEN));

        Action finalAction = Action.finish(result_code, finishingData);

        return finalAction;
    }

    public EmailFlowController (Context context, String appName) {
        this.mAppContext = context;
        this.mAppName = appName;
    }
}

