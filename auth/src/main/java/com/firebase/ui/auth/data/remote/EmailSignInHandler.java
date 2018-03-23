package com.firebase.ui.auth.data.remote;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.email.EmailActivity;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.firebase.ui.auth.viewmodel.idp.ProviderSignInBase;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailSignInHandler extends ProviderSignInBase<Void> {
    public EmailSignInHandler(Application application) {
        super(application);
    }

    @Override
    public void startSignIn(@NonNull HelperActivityBase activity) {
        activity.startActivityForResult(
                EmailActivity.createIntent(activity, activity.getFlowParams()),
                RequestCodes.EMAIL_FLOW);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RequestCodes.EMAIL_FLOW) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (response == null) {
                setResult(Resource.<IdpResponse>forFailure(new UserCancellationException()));
            } else if (response.isSuccessful()){
                setResult(Resource.forSuccess(response));
            } else {
                // TODO: Will this correctly pass back anonymous errors?
                setResult(Resource.<IdpResponse>forFailure(response.getError()));
            }
        }
    }
}
