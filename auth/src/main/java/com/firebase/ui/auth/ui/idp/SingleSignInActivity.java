package com.firebase.ui.auth.ui.idp;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
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
import com.firebase.ui.auth.data.remote.FacebookSignInHandler;
import com.firebase.ui.auth.data.remote.GoogleSignInHandler;
import com.firebase.ui.auth.data.remote.TwitterSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.ui.FlowUtils;
import com.firebase.ui.auth.viewmodel.idp.ProviderSignInBase;
import com.firebase.ui.auth.viewmodel.idp.SocialProviderResponseHandler;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

public class SingleSignInActivity extends HelperActivityBase {
    private SocialProviderResponseHandler mHandler;
    private ProviderSignInBase<?> mProvider;

    public static Intent createIntent(Context context, FlowParameters flowParams, User user) {
        return HelperActivityBase.createBaseIntent(context, SingleSignInActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_USER, user);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = User.getUser(getIntent());
        String provider = user.getProviderId();

        AuthUI.IdpConfig providerConfig =
                ProviderUtils.getConfigFromIdps(getFlowParams().providerInfo, provider);
        if (providerConfig == null) {
            finish(RESULT_CANCELED, IdpResponse.getErrorIntent(new FirebaseUiException(
                    ErrorCodes.DEVELOPER_ERROR,
                    "Provider not enabled: " + provider)));
            return;
        }

        ViewModelProvider supplier = ViewModelProviders.of(this);

        mHandler = supplier.get(SocialProviderResponseHandler.class);
        mHandler.init(getFlowParams());

        switch (provider) {
            case GoogleAuthProvider.PROVIDER_ID:
                GoogleSignInHandler google = supplier.get(GoogleSignInHandler.class);
                google.init(new GoogleSignInHandler.Params(providerConfig, user.getEmail()));
                mProvider = google;
                break;
            case FacebookAuthProvider.PROVIDER_ID:
                FacebookSignInHandler facebook = supplier.get(FacebookSignInHandler.class);
                facebook.init(providerConfig);
                mProvider = facebook;
                break;
            case TwitterAuthProvider.PROVIDER_ID:
                TwitterSignInHandler twitter = supplier.get(TwitterSignInHandler.class);
                twitter.init(null);
                mProvider = twitter;
                break;
            default:
                throw new IllegalStateException("Invalid provider id: " + provider);
        }

        mProvider.getOperation().observe(this, new Observer<Resource<IdpResponse>>() {
            @Override
            public void onChanged(Resource<IdpResponse> resource) {
                if (resource.getState() == State.LOADING) {
                    getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_loading);
                    return;
                }
                getDialogHolder().dismissDialog();

                if (resource.getState() == State.SUCCESS
                        || resource.getState() == State.FAILURE) {
                    mHandler.startSignIn(IdpResponse.from(resource));
                }
            }
        });

        mHandler.getOperation().observe(this, new Observer<Resource<IdpResponse>>() {
            @Override
            public void onChanged(Resource<IdpResponse> resource) {
                if (resource.getState() == State.LOADING) {
                    getDialogHolder().showLoadingDialog(R.string.fui_progress_dialog_loading);
                    return;
                }
                getDialogHolder().dismissDialog();

                if (resource.getState() == State.SUCCESS) {
                    startSaveCredentials(mHandler.getCurrentUser(), null, resource.getValue());
                } else if (resource.getState() == State.FAILURE) {
                    Exception e = resource.getException();
                    if (!FlowUtils.handleError(SingleSignInActivity.this, e)) {
                        finish(RESULT_CANCELED, IdpResponse.getErrorIntent(e));
                    }
                }
            }
        });

        if (mHandler.getOperation().getValue() == null) {
            mProvider.startSignIn(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mHandler.onActivityResult(requestCode, resultCode, data);
        mProvider.onActivityResult(requestCode, resultCode, data);
    }
}
