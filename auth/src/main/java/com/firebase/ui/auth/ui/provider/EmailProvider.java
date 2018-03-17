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
import com.firebase.ui.auth.ui.email.EmailActivity;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.firebase.ui.auth.viewmodel.idp.ProvidersHandlerBase;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailProvider extends ProviderBase {
    public EmailProvider(ProvidersHandlerBase handler) {
        super(handler);
    }

    @NonNull
    @Override
    public String getName() {
        return AuthUI.getApplicationContext().getString(R.string.fui_provider_name_email);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_provider_button_email;
    }

    @Override
    public void startLogin(@NonNull HelperActivityBase activity) {
        activity.startActivityForResult(
                EmailActivity.createIntent(activity, activity.getFlowParams()),
                RequestCodes.EMAIL_FLOW);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RequestCodes.EMAIL_FLOW) {
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
