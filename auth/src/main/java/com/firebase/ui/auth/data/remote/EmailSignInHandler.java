package com.firebase.ui.auth.data.remote;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.email.EmailActivity;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;

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
        if (resultCode == ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT) {
            // The activity deals with this case. This conflict is handled by the developer.
        } else if (requestCode == RequestCodes.EMAIL_FLOW) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (response == null) {
                setResult(Resource.<IdpResponse>forFailure(new UserCancellationException()));
            } else {
                setResult(Resource.forSuccess(response));
            }
        }
    }
}
