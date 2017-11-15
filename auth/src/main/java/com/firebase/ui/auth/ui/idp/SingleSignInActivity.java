package com.firebase.ui.auth.ui.idp;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.ProviderDisabledException;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.provider.FacebookProvider;
import com.firebase.ui.auth.ui.provider.GoogleProvider;
import com.firebase.ui.auth.ui.provider.Provider;
import com.firebase.ui.auth.ui.provider.TwitterProvider;
import com.firebase.ui.auth.util.ExtraConstants;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

public class SingleSignInActivity extends HelperActivityBase {
    public static Intent createIntent(Context context, FlowParameters flowParams, User user) {
        return HelperActivityBase.createBaseIntent(context, SingleSignInActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_USER, user);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSignInHandler().getSignInLiveData().observe(this, new Observer<IdpResponse>() {
            @Override
            public void onChanged(@Nullable IdpResponse response) {
                finish(response.isSuccessful() ? Activity.RESULT_OK : Activity.RESULT_CANCELED,
                       response.toIntent());
            }
        });
        getFlowHolder().getProgressListener().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean isDone) {
                Toast.makeText(SingleSignInActivity.this,
                        "TODO isDone:  " + isDone,
                        Toast.LENGTH_SHORT).show();
            }
        });

        if (savedInstanceState == null) {
            startLogin();
        }
    }

    private void startLogin() {
        User user = User.getUser(getIntent());
        String provider = user.getProviderId();

        AuthUI.IdpConfig providerConfig = null;
        for (AuthUI.IdpConfig config : getFlowHolder().getParams().providerInfo) {
            if (config.getProviderId().equals(provider)) {
                providerConfig = config;
                break;
            }
        }

        if (providerConfig == null) {
            // we don't have a provider to handle this
            finish(Activity.RESULT_CANCELED,
                   IdpResponse.fromError(new ProviderDisabledException(provider)).toIntent());
            return;
        }

        Provider uiProvider;
        switch (provider) {
            case GoogleAuthProvider.PROVIDER_ID:
                uiProvider = new GoogleProvider(this, providerConfig, user.getEmail());
                break;
            case FacebookAuthProvider.PROVIDER_ID:
                uiProvider = new FacebookProvider(this, providerConfig);
                break;
            case TwitterAuthProvider.PROVIDER_ID:
                uiProvider = new TwitterProvider(this);
                break;
            default:
                throw new IllegalStateException(
                        "Provider config id does not equal Firebase auth one");
        }

        uiProvider.startLogin(this);
    }
}
