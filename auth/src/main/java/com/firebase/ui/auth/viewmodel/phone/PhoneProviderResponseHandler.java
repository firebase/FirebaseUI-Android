package com.firebase.ui.auth.viewmodel.phone;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PhoneProviderResponseHandler extends AuthViewModelBase<IdpResponse> {
    public PhoneProviderResponseHandler(Application application) {
        super(application);
    }

    public void startSignIn(@NonNull PhoneAuthCredential credential,
                            @NonNull final IdpResponse response) {
        if (!response.isSuccessful()) {
            setResult(Resource.<IdpResponse>forFailure(response.getError()));
            return;
        }
        if (!response.getProviderType().equals(PhoneAuthProvider.PROVIDER_ID)) {
            throw new IllegalStateException(
                    "This handler cannot be used without a phone response.");
        }
        setResult(Resource.<IdpResponse>forLoading());

        getAuth().signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            setResult(Resource.forSuccess(response.withResult(task.getResult())));
                        } else {
                            setResult(Resource.<IdpResponse>forFailure(task.getException()));
                        }
                    }
                });
    }
}
