package com.firebase.ui.auth.viewmodel.idp;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProviderResponseHandlerBase extends AuthViewModelBase<IdpResponse> {
    public ProviderResponseHandlerBase(Application application) {
        super(application);
    }

    public void startSignIn(@NonNull IdpResponse inputResponse) {
        if (checkForInvalidSignInState(inputResponse)) return;
        startSignIn(ProviderUtils.getAuthCredential(inputResponse), inputResponse);
    }

    /** Kick off the sign-in process. */
    public void startSignIn(@NonNull AuthCredential credential,
                            @NonNull final IdpResponse inputResponse) {
        if (checkForInvalidSignInState(inputResponse)) return;
        setResult(Resource.<IdpResponse>forLoading());
        signIn(credential, inputResponse);
    }

    private boolean checkForInvalidSignInState(@NonNull IdpResponse inputResponse) {
        if (!inputResponse.isSuccessful()) {
            setResult(Resource.<IdpResponse>forFailure(inputResponse.getError()));
            return true;
        }
        if (inputResponse.getProviderType().equals(EmailAuthProvider.PROVIDER_ID)
                || inputResponse.getProviderType().equals(PhoneAuthProvider.PROVIDER_ID)) {
            setResult(Resource.forSuccess(inputResponse));
            return true;
        }
        return false;
    }

    protected abstract void signIn(
            @NonNull AuthCredential credential, @NonNull IdpResponse response);
}

