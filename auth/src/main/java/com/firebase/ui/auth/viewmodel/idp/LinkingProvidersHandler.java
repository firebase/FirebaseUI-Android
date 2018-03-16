package com.firebase.ui.auth.viewmodel.idp;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.AuthCredential;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LinkingProvidersHandler extends ProvidersHandlerBase {
    private IdpResponse mAttemptedSignInResponse;

    public LinkingProvidersHandler(Application application) {
        super(application);
    }

    public void setAttemptedSignInResponse(@NonNull IdpResponse response) {
        mAttemptedSignInResponse = response;
    }

    @Override
    protected void signIn(@NonNull AuthCredential credential,
                          @NonNull final IdpResponse inputResponse) {
    }
}
