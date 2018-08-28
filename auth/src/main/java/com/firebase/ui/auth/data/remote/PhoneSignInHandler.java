package com.firebase.ui.auth.data.remote;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.phone.PhoneActivity;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PhoneSignInHandler extends ProviderSignInBase<AuthUI.IdpConfig> {
    public PhoneSignInHandler(Application application) {
        super(application);
    }

    @Override
    public void startSignIn(@NonNull HelperActivityBase activity) {
        activity.startActivityForResult(
                PhoneActivity.createIntent(
                        activity, activity.getFlowParams(), getArguments().getParams()),
                RequestCodes.PHONE_FLOW);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RequestCodes.PHONE_FLOW) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (response == null) {
                setResult(Resource.<IdpResponse>forFailure(new UserCancellationException()));
            } else {
                setResult(Resource.forSuccess(response));
            }
        }
    }
}
