package com.firebase.ui.auth.ui.idp;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.provider.FacebookProvider;
import com.firebase.ui.auth.ui.provider.GoogleProvider;
import com.firebase.ui.auth.ui.provider.Provider;
import com.firebase.ui.auth.ui.provider.TwitterProvider;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.ui.FlowUtils;
import com.firebase.ui.auth.viewmodel.idp.SimpleProvidersHandler;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

public class SingleSignInActivity extends HelperActivityBase {
    private SimpleProvidersHandler mHandler;
    private Provider mProvider;

    public static Intent createIntent(Context context, FlowParameters flowParams, User user) {
        return HelperActivityBase.createBaseIntent(context, SingleSignInActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_USER, user);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            startLogin();
        }
    }

    private void startLogin() {
        User user = User.getUser(getIntent());
        String provider = user.getProviderId();

        AuthUI.IdpConfig providerConfig =
                ProviderUtils.getConfigFromIdps(getFlowParams().providerInfo, provider);
        if (providerConfig == null) {
            // we don't have a provider to handle this
            finish(Activity.RESULT_CANCELED, IdpResponse.getErrorIntent(
                    new FirebaseUiException(ErrorCodes.PROVIDER_ERROR,
                            "Provider not enabled: " + provider)));
            return;
        }

        mHandler = ViewModelProviders.of(this).get(SimpleProvidersHandler.class);
        mHandler.init(getFlowParams());

        switch (provider) {
            case GoogleAuthProvider.PROVIDER_ID:
                mProvider = new GoogleProvider(mHandler, this, user.getEmail());
                break;
            case FacebookAuthProvider.PROVIDER_ID:
                mProvider = new FacebookProvider(mHandler, this);
                break;
            case TwitterAuthProvider.PROVIDER_ID:
                mProvider = new TwitterProvider(mHandler, this);
                break;
            default:
                throw new IllegalStateException(
                        "Provider config id does not equal Firebase auth one");
        }

        mHandler.getOperation().observe(this, new Observer<Resource<IdpResponse>>() {
            @Override
            public void onChanged(Resource<IdpResponse> resource) {
                if (resource.getState() == State.LOADING) {
                    getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_loading);
                    return;
                }
                getDialogHolder().dismissDialog();

                if (resource.isUsed()) { return; }

                if (resource.getState() == State.SUCCESS) {
                    startSaveCredentials(mHandler.getCurrentUser(), null, resource.getValue());
                } else {
                    Exception e = resource.getException();
                    if (!FlowUtils.handleError(SingleSignInActivity.this, e)) {
                        finish(RESULT_CANCELED, IdpResponse.getErrorIntent(e));
                    }
                }
            }
        });
        mProvider.startLogin(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mHandler.onActivityResult(requestCode, resultCode, data);
        mProvider.onActivityResult(requestCode, resultCode, data);
    }
}
