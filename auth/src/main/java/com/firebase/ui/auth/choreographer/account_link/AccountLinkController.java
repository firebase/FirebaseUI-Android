package com.firebase.ui.auth.choreographer.account_link;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.firebase.ui.auth.api.HeadlessAPIWrapper;
import com.firebase.ui.auth.choreographer.Action;
import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.Result;
import com.firebase.ui.auth.api.FactoryHeadlessAPI;
import com.firebase.ui.auth.choreographer.idp.provider.FacebookProvider;
import com.firebase.ui.auth.choreographer.idp.provider.GoogleProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPResponse;
import com.firebase.ui.auth.ui.BaseActivity;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.firebase.ui.auth.ui.account_link.WelcomeBackIDPPrompt;
import com.firebase.ui.auth.ui.account_link.WelcomeBackPasswordPrompt;
import com.firebase.ui.auth.ui.email.RecoverPasswordActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class AccountLinkController implements Controller {
    private Context mContext;
    private static final String TAG = "AccountLinkController";

    // states
    public static final int ID_INIT = 10;
    public static final int ID_WELCOME_BACK_PASSWORD = 20;
    public static final int ID_WELCOME_BACK_IDP = 30;
    @VisibleForTesting static final int ID_CREDENTIALS_SAVE = 40;


    public AccountLinkController(Context context) {
        mContext = context;
    }

    @Override
    public Action next(Result result) {
        Intent data = result.getData();

        String email = data.getStringExtra(ControllerConstants.EXTRA_EMAIL);
        String password = data.getStringExtra(ControllerConstants.EXTRA_PASSWORD);
        String provider = data.getStringExtra(ControllerConstants.EXTRA_PROVIDER);
        String appName = data.getStringExtra(ControllerConstants.EXTRA_APP_NAME);

        IDPResponse idpResponse = data.getParcelableExtra(ControllerConstants.EXTRA_IDP_RESPONSE);
        FirebaseUser currentUser;
        HeadlessAPIWrapper apiWrapper = FactoryHeadlessAPI.getHeadlessAPIWrapperInstance(appName);

        switch (result.getId()) {
            case ID_INIT:
                if (email == null) {
                   finishAction(Activity.RESULT_OK);
                }
                List<String> providers = apiWrapper.getProviderList(email);

                if (providers.size() == 0) {
                    // new account for this email
                    return Action.next(
                            ID_CREDENTIALS_SAVE,
                            SaveCredentialsActivity.createIntent(mContext, null, email, password,
                                    provider, null, appName));
                } else if (providers.size() == 1)  {
                    if (providers.get(0).equals(provider)) {
                        // existing account but has this IDP linked
                        return Action.next(
                                ID_CREDENTIALS_SAVE,
                                new Intent(mContext, SaveCredentialsActivity.class)
                                        .putExtra(ControllerConstants.EXTRA_EMAIL, email)
                                        .putExtra(ControllerConstants.EXTRA_PROVIDER, provider)
                                        .putExtra(ControllerConstants.EXTRA_PASSWORD, password));
                    } else {
                        if (providers.get(0).equals(EmailAuthProvider.PROVIDER_ID)) {
                            return Action.next(
                                    ID_WELCOME_BACK_PASSWORD,
                                    new Intent(mContext, WelcomeBackPasswordPrompt.class)
                                        .putExtra(ControllerConstants.EXTRA_EMAIL, email)
                                        .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
                            );
                        } else {
                            // existing account but has a different IDP linked
                            return Action.next(
                                    ID_WELCOME_BACK_IDP,
                                    new Intent(mContext, WelcomeBackIDPPrompt.class)
                                            .putExtra(ControllerConstants.EXTRA_EMAIL, email)
                                            .putExtra(ControllerConstants.EXTRA_PROVIDER, provider)
                            );
                        }
                    }
                } else {
                    // more than one providers
                    return Action.next(
                            ID_WELCOME_BACK_IDP,
                            new Intent(mContext, WelcomeBackIDPPrompt.class)
                                    .putExtra(ControllerConstants.EXTRA_EMAIL, email)
                    );
                }
            case ID_WELCOME_BACK_IDP:
                if (result.getResultCode() == BaseActivity.BACK_IN_FLOW) {
                    return finishAction(Activity.RESULT_CANCELED);
                }
                AuthCredential credential;
                switch (provider) {
                    case GoogleAuthProvider.PROVIDER_ID:
                        credential = GoogleProvider.createAuthCredential(idpResponse);
                        break;
                    case FacebookAuthProvider.PROVIDER_ID:
                        credential = FacebookProvider.createAuthCredential(idpResponse);
                        break;
                    default:
                        Log.e(TAG, "Unknown provider: " + provider);
                        return finishAction(Activity.RESULT_FIRST_USER);
                }
                currentUser = apiWrapper.getCurrentUser();
                if (currentUser == null) {
                    apiWrapper.signInWithCredential(credential);
                } else {
                    try {
                        apiWrapper.linkWithCredential(currentUser, credential);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                return finishAction(Activity.RESULT_OK);

            case ID_WELCOME_BACK_PASSWORD:
                if (result.getResultCode() == BaseActivity.BACK_IN_FLOW) {
                    return finishAction(Activity.RESULT_OK);
                }
                if (result.getResultCode() == BaseActivity.RESULT_FIRST_USER) {
                    return Action.startFlow(RecoverPasswordActivity
                            .createIntent(mContext, appName, email));
                }
                currentUser = apiWrapper.getCurrentUser();
                AuthCredential emailCredential = EmailAuthProvider.getCredential(email, password);
                if (currentUser != null) {
                    try {
                        apiWrapper.linkWithCredential(currentUser, emailCredential);
                    } catch (ExecutionException e) {
                        return Action.next(ID_WELCOME_BACK_PASSWORD,
                                new Intent(mContext, WelcomeBackPasswordPrompt.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                        .putExtra(ControllerConstants.EXTRA_EMAIL, email)
                                        .putExtra(ControllerConstants.EXTRA_ERROR, e.toString()));
                    }
                }

                FirebaseUser passwordUser = null;
                if (!password.isEmpty()) {
                    passwordUser = apiWrapper.signInWithEmailPassword(email, password);
                }
                if (passwordUser == null) {
                    return Action.next(
                             ID_WELCOME_BACK_PASSWORD,
                             new Intent(mContext, WelcomeBackPasswordPrompt.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                .putExtra(ControllerConstants.EXTRA_EMAIL, email)
                                .putExtra(ControllerConstants.EXTRA_ERROR, "Password is incorrect")
                    );
                } else {
                    return finishAction(Activity.RESULT_OK);
                }
            case ID_CREDENTIALS_SAVE:
                return finishAction(Activity.RESULT_OK);
        }
        return finishAction(Activity.RESULT_FIRST_USER);
    }

    private Action finishAction(int resultCode) {
        return Action.finish(resultCode, new Intent());
    }
}
