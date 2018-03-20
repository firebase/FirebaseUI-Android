package com.firebase.ui.auth.ui.provider;

import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.email.EmailActivity;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.firebase.ui.auth.viewmodel.idp.ProviderResponseHandlerBase;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailProvider extends ProviderBase {
    public EmailProvider(ProviderResponseHandlerBase handler) {
        super(handler);
    }

    @StringRes
    @Override
    public int getNameRes() {
        return R.string.fui_provider_name_email;
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
                        new UserCancellationException()));
            } else {
                getProvidersHandler().startSignIn(response);
            }
        }
    }
}
