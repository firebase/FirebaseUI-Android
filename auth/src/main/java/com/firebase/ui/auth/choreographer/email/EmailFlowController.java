package com.firebase.ui.auth.choreographer.email;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.api.FactoryHeadlessAPI;
import com.firebase.ui.auth.api.HeadlessAPIWrapper;
import com.firebase.ui.auth.choreographer.Action;
import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.Result;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.firebase.ui.auth.ui.account_link.WelcomeBackIDPPrompt;
import com.firebase.ui.auth.ui.email.EmailFlowBaseActivity;
import com.firebase.ui.auth.ui.email.ConfirmRecoverPasswordActivity;

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
        HeadlessAPIWrapper apiWrapper = FactoryHeadlessAPI.getHeadlessAPIWrapperInstance
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
                boolean isAccountExists = apiWrapper.isAccountExists(email);
                if(!isAccountExists) {
                    Intent registerIntent = new Intent(mAppContext, RegisterEmailActivity.class);
                    registerIntent.putExtra(ControllerConstants.EXTRA_EMAIL, email);
                    return Action.next(ID_REGISTER_EMAIL, registerIntent);
                }
                List<String> providers =
                        apiWrapper.getProviderList(email);

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
                    return handleSuccessfulLogin(email, password, firebaseUser);
                }
            case ID_REGISTER_EMAIL:
                if (result.getResultCode() == EmailFlowBaseActivity.BACK_IN_FLOW) {
                    Intent intent = new Intent(mAppContext, SignInNoPasswordActivity.class);
                    intent.putExtra(ControllerConstants.EXTRA_EMAIL, email);
                    return Action.back(ID_SELECT_EMAIL, intent);
                }
                FirebaseUser firebaseUser = apiWrapper.createEmailWithPassword(email, password);
                if (firebaseUser == null) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Error creating account!");
                    }
                    return finish(Activity.RESULT_FIRST_USER, (FirebaseUser) null);
                }
                return handleSuccessfulLogin(email, password, firebaseUser);
            case ID_RECOVER_PASSWORD:
                if (result.getResultCode() == EmailFlowBaseActivity.BACK_IN_FLOW) {
                    signInIntent = new Intent(mAppContext, SignInActivity.class);
                    signInIntent.putExtra(ControllerConstants.EXTRA_EMAIL, email);
                    return Action.back(ID_SIGN_IN, signInIntent);
                }
                boolean isSuccess =
                        apiWrapper.resetEmailPassword(email);
                Intent confirmIntent = new Intent(mAppContext,
                        ConfirmRecoverPasswordActivity.class);
                confirmIntent.putExtra(ControllerConstants.EXTRA_SUCCESS, isSuccess);
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

    private Action handleSuccessfulLogin(String email, String password, FirebaseUser firebaseUser) {
        if (firebaseUser != null) {
            if(FactoryHeadlessAPI.getHeadlessAPIWrapperInstance(ControllerConstants.APP_NAME)
                    .isGMSCorePresent(mAppContext)) {
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
            return finish(Activity.RESULT_FIRST_USER, firebaseUser);
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

