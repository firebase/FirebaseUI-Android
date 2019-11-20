package com.firebase.ui.auth.ui.idp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.FacebookSignInHandler;
import com.firebase.ui.auth.data.remote.GenericIdpSignInHandler;
import com.firebase.ui.auth.data.remote.GoogleSignInHandler;
import com.firebase.ui.auth.ui.InvisibleActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.firebase.ui.auth.viewmodel.idp.SocialProviderResponseHandler;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import static com.firebase.ui.auth.util.ExtraConstants.GENERIC_OAUTH_PROVIDER_ID;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SingleSignInActivity extends InvisibleActivityBase {
    private SocialProviderResponseHandler mHandler;
    private ProviderSignInBase<?> mProvider;

    public static Intent createIntent(Context context, FlowParameters flowParams, User user) {
        return createBaseIntent(context, SingleSignInActivity.class, flowParams)
                .putExtra(ExtraConstants.USER, user);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = User.getUser(getIntent());
        final String provider = user.getProviderId();

        AuthUI.IdpConfig providerConfig =
                ProviderUtils.getConfigFromIdps(getFlowParams().providers, provider);
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
            default:
                if (!TextUtils.isEmpty(
                        providerConfig.getParams().getString(GENERIC_OAUTH_PROVIDER_ID))) {
                    GenericIdpSignInHandler genericIdp =
                            supplier.get(GenericIdpSignInHandler.class);
                    genericIdp.init(providerConfig);
                    mProvider = genericIdp;

                    break;
                }
                throw new IllegalStateException("Invalid provider id: " + provider);
        }

        mProvider.getOperation().observe(this, new ResourceObserver<IdpResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdpResponse response) {
                if (AuthUI.SOCIAL_PROVIDERS.contains(provider) || !response.isSuccessful()) {
                    mHandler.startSignIn(response);
                    return;
                }
                finish(response.isSuccessful() ? RESULT_OK : RESULT_CANCELED,
                        response.toIntent());
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                if (e instanceof FirebaseAuthAnonymousUpgradeException) {
                    finish(RESULT_CANCELED, new Intent().putExtra(ExtraConstants.IDP_RESPONSE,
                            IdpResponse.from(e)));
                    return;
                }
                mHandler.startSignIn(IdpResponse.from(e));
            }
        });

        mHandler.getOperation().observe(this, new ResourceObserver<IdpResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdpResponse response) {
                startSaveCredentials(mHandler.getCurrentUser(), response, null);
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                if (e instanceof FirebaseAuthAnonymousUpgradeException) {
                    IdpResponse res = ((FirebaseAuthAnonymousUpgradeException) e).getResponse();
                    finish(RESULT_CANCELED, new Intent().putExtra(ExtraConstants.IDP_RESPONSE, res));
                } else {
                    finish(RESULT_CANCELED, IdpResponse.getErrorIntent(e));
                }
            }
        });

        if (mHandler.getOperation().getValue() == null) {
            FirebaseAuth auth = FirebaseAuth.getInstance(
                    FirebaseApp.getInstance(getFlowParams().appName));
            mProvider.startSignIn(auth,
                    this, provider);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mHandler.onActivityResult(requestCode, resultCode, data);
        mProvider.onActivityResult(requestCode, resultCode, data);
    }
}
