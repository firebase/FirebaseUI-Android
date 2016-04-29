package com.firebase.ui.auth.choreographer.idp;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.ui.auth.api.FactoryHeadlessAPI;
import com.firebase.ui.auth.api.HeadlessAPIWrapper;
import com.firebase.ui.auth.choreographer.Action;
import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.Result;
import com.firebase.ui.auth.choreographer.idp.provider.FacebookProvider;
import com.firebase.ui.auth.choreographer.idp.provider.GoogleProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.firebase.ui.auth.choreographer.idp.provider.IDPResponse;
import com.firebase.ui.auth.ui.BaseActivity;
import com.firebase.ui.auth.ui.account_link.AccountLinkInitActivity;
import com.firebase.ui.auth.ui.email.EmailHintContainerActivity;
import com.firebase.ui.auth.ui.idp.IDPBaseActivity;
import com.firebase.ui.auth.ui.idp.NascarActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.List;


public class IDPController implements Controller{
    public static final int ID_INIT = 10;
    public static final int NASCAR_SCREEN = 20;
    private final Context mContext;
    private final String mAppName;
    private static final String TAG = "IDPController";

    @Override
    public Action next(Result result) {
        if(result == null) {
            return Action.next(ID_INIT, new Intent(mContext, NascarActivity.class));
        }
        IDPResponse idpSignInResponse
                = result.getData().getParcelableExtra(ControllerConstants.EXTRA_IDP_RESPONSE);
        HeadlessAPIWrapper apiWrapper
                = FactoryHeadlessAPI.getHeadlessAPIWrapperInstance(ControllerConstants.APP_NAME);
        FirebaseUser user = null;
        switch(result.getId()) {
            case DEFAULT_INIT_FLOW_ID:
            case ID_INIT:
                switch (result.getResultCode()) {
                    case BaseActivity.RESULT_OK :
                        AuthCredential credential = createCredential(idpSignInResponse);
                        user = apiWrapper.signInWithCredential(credential);
                        if (user == null) {
                            return getNascarAction(result);
                        }
                        break;
                    case  IDPBaseActivity.EMAIL_LOGIN_NEEDED :
                        break;
                    case IDPBaseActivity.LOGIN_CANCELLED:
                    case BaseActivity.RESULT_CANCELED:
                        return getNascarAction(result);
                    case BaseActivity.BACK_IN_FLOW:
                        return finish(BaseActivity.RESULT_CANCELED, null);
                    default:
                        throw new IllegalStateException(
                                String.format("NASCAR screen gets unexpected resultCode %d.",
                                        result.getResultCode()));
                }
                return finish(result.getResultCode(), user);
            case NASCAR_SCREEN:
                switch (result.getResultCode()) {
                    case BaseActivity.RESULT_OK :
                        AuthCredential credential = createCredential(idpSignInResponse);
                        user = apiWrapper.signInWithCredential(credential);
                        if (user == null) {
                            Intent data = new Intent();
                            Log.w(TAG, "Firebase login unsuccessful");
                            data.putExtra(ControllerConstants.EXTRA_ERROR_MESSAGE,
                                    "Firebase login unsuccessful");
                            return Action.block(data);
                        }
                        break;
                    case  IDPBaseActivity.EMAIL_LOGIN_NEEDED :
                        break;
                    case IDPBaseActivity.LOGIN_CANCELLED:
                        Intent data = result.getData();
                        data.putExtra(ControllerConstants.EXTRA_ERROR_MESSAGE,
                                "IDP login unsuccessful");
                        return Action.block(data);
                    case BaseActivity.RESULT_CANCELED:
                    case BaseActivity.BACK_IN_FLOW:
                        return finish(BaseActivity.RESULT_CANCELED, null);
                    default:
                        throw new IllegalStateException(
                                String.format("NASCAR screen gets unexpected resultCode %d.",
                                        result.getResultCode()));
                }
                return finish(result.getResultCode(), user);
            default:
                // Otherwise throw an error since we can not recognize intended action
                throw new IllegalStateException(
                        String.format("Result not handled with id %d and resultCode %d.",
                                result.getId(),
                                result.getResultCode()));
        }
    }

    @NonNull
    private Action getNascarAction(Result result) {
        ArrayList<IDPProviderParcel> parcels = result.getData().getParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS);
        Intent intent = NascarActivity.createIntent(mContext, mAppName, parcels);
        return Action.next(NASCAR_SCREEN, intent);
    }

    private Action finish(int result_code, FirebaseUser firebaseUser) {
        Intent intent;
        switch (result_code) {
            case IDPBaseActivity.EMAIL_LOGIN_NEEDED:
                intent = EmailHintContainerActivity.getInitIntent(mContext, mAppName);
                return Action.startFlow(intent);
            case BaseActivity.RESULT_OK:
                List<String> providers = firebaseUser.getProviders();
                String provider = null;
                if (providers.size() == 1) {
                    provider = providers.get(0);
                } else {
                    Log.e(TAG, "Expecting a single provider, received :" + providers.size());
                }
                return Action.startFlow(AccountLinkInitActivity.createStartIntent(mContext, mAppName, firebaseUser
                        .getEmail(), provider));
            default:
                return Action.finish(BaseActivity.RESULT_CANCELED, new Intent());
        }
    }

    private AuthCredential createCredential(IDPResponse idpSignInResponse) {
        if (idpSignInResponse.getProviderType().equalsIgnoreCase(FacebookAuthProvider.PROVIDER_ID)) {
            return FacebookProvider.createAuthCredential(idpSignInResponse);
        } else if (idpSignInResponse.getProviderType().equalsIgnoreCase(GoogleAuthProvider
                .PROVIDER_ID)) {
            return GoogleProvider.createAuthCredential(idpSignInResponse);
        }

        return null;
    }

    public IDPController(Context context, String appName) {
        mContext = context;
        mAppName = appName;
    }
}


