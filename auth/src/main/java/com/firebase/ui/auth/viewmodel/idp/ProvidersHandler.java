package com.firebase.ui.auth.viewmodel.idp;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProvidersHandler extends AuthViewModelBase<IdpResponse> {
    public ProvidersHandler(Application application) {
        super(application);
    }

    public void loading() {
        setResult(Resource.<IdpResponse>forLoading());
    }

    /** Kick off the sign-in process. */
    public void startSignIn(@NonNull IdpResponse inputResponse) {
        if (!inputResponse.isSuccessful()) {
            setResult(Resource.<IdpResponse>forFailure(inputResponse.getException()));
            return;
        }
        if (inputResponse.getProviderType().equals(EmailAuthProvider.PROVIDER_ID)
                || inputResponse.getProviderType().equals(PhoneAuthProvider.PROVIDER_ID)) {
            setResult(Resource.forSuccess(inputResponse));
            return;
        }

        TODO;
    }
}

