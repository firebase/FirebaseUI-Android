package com.firebase.ui.auth.ui.provider;

import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.phone.PhoneActivity;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.firebase.ui.auth.viewmodel.idp.ProvidersHandlerBase;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PhoneProvider extends ProviderBase {
    private final AuthUI.IdpConfig mConfig;

    public PhoneProvider(ProvidersHandlerBase handler, AuthUI.IdpConfig config) {
        super(handler);
        mConfig = config;
    }

    @NonNull
    @Override
    public String getName() {
        return AuthUI.getApplicationContext().getString(R.string.fui_provider_name_phone);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_provider_button_phone;
    }

    @Override
    public void startLogin(@NonNull HelperActivityBase activity) {
        activity.startActivityForResult(
                PhoneActivity.createIntent(
                        activity, activity.getFlowParams(), mConfig.getParams()),
                RequestCodes.RC_PHONE_FLOW);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RequestCodes.RC_PHONE_FLOW) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (response == null) {
                getProvidersHandler().startSignIn(IdpResponse.fromError(
                        new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR)));
            } else {
                getProvidersHandler().startSignIn(response);
            }
        }
    }
}
