package com.firebase.ui.auth.viewmodel.phone;

import android.app.Application;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.viewmodel.SignInViewModelBase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PhoneProviderResponseHandler extends SignInViewModelBase {
    public PhoneProviderResponseHandler(Application application) {
        super(application);
    }

    public void startSignIn(@NonNull PhoneAuthCredential credential,
                            @NonNull final IdpResponse response) {
        if (!response.isSuccessful()) {
            setResult(Resource.forFailure(response.getError()));
            return;
        }
        if (!response.getProviderType().equals(PhoneAuthProvider.PROVIDER_ID)) {
            throw new IllegalStateException(
                    "This handler cannot be used without a phone response.");
        }

        setResult(Resource.forLoading());

        AuthOperationManager.getInstance()
                .signInAndLinkWithCredential(getAuth(), getArguments(), credential)
                .addOnSuccessListener(result -> handleSuccess(response, result))
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        // With phone auth, this only happens if we are trying to upgrade
                        // an anonymous account using a phone number that is already registered
                        // on another account
                        handleMergeFailure(((FirebaseAuthUserCollisionException) e).getUpdatedCredential());
                    } else {
                        setResult(Resource.forFailure(e));
                    }
                });
    }
}
