package com.firebase.ui.auth.viewmodel;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class SignInViewModelBase extends AuthViewModelBase<IdpResponse> {
    protected SignInViewModelBase(Application application) {
        super(application);
    }

    @Override
    protected void setResult(Resource<IdpResponse> output) {
        super.setResult(output);
    }

    protected void handleSuccess(@NonNull IdpResponse response, @NonNull AuthResult result) {
        setResult(Resource.forSuccess(response.withResult(result)));
    }

    protected void handleMergeFailure(@NonNull AuthCredential credential) {
        IdpResponse failureResponse = new IdpResponse.Builder(credential).build();
        handleMergeFailure(failureResponse);
    }

    protected void handleMergeFailure(@NonNull IdpResponse failureResponse) {
        setResult(Resource.<IdpResponse>forFailure(new FirebaseAuthAnonymousUpgradeException(
                ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT,
                failureResponse)));
    }

}
