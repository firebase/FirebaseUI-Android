package com.firebase.ui.auth.viewmodel.idp;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.remote.ProfileMerger;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
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
    public void startSignIn(@NonNull final IdpResponse inputResponse) {
        if (!inputResponse.isSuccessful()) {
            setResult(Resource.<IdpResponse>forFailure(inputResponse.getError()));
            return;
        }
        if (inputResponse.getProviderType().equals(EmailAuthProvider.PROVIDER_ID)
                || inputResponse.getProviderType().equals(PhoneAuthProvider.PROVIDER_ID)) {
            setResult(Resource.forSuccess(inputResponse));
            return;
        }
        loading();

        AuthCredential credential = ProviderUtils.getAuthCredential(inputResponse);
        Task<AuthResult> signIn;
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            signIn = getAuth().signInWithCredential(credential);
        } else {
            signIn = currentUser.linkWithCredential(credential);
        }
        signIn.continueWithTask(new ProfileMerger(inputResponse))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        setResult(Resource.forSuccess(inputResponse));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            // TODO return the auth credential for phase 1
                        }
                        setResult(Resource.<IdpResponse>forFailure(e));
                    }
                });
    }
}

