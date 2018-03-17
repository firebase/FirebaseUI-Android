package com.firebase.ui.auth.viewmodel.idp;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LinkingProvidersHandler extends ProvidersHandlerBase {
    private AuthCredential mRequestedSignInCredential;

    public LinkingProvidersHandler(Application application) {
        super(application);
    }

    public void setRequestedSignInCredential(@Nullable AuthCredential credential) {
        mRequestedSignInCredential = credential;
    }

    @Override
    protected void signIn(@NonNull AuthCredential credential,
                          @NonNull final IdpResponse response) {
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            getAuth().signInWithCredential(credential)
                    .addOnSuccessListener(new StartLink(response))
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            setResult(Resource.<IdpResponse>forFailure(e));
                        }
                    });
        } else {
            currentUser
                    .linkWithCredential(credential)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // I'm not sure why we ignore failures here, but this mirrors previous
                            // behavior.
                            setResult(Resource.forSuccess(response));
                        }
                    });
        }
    }

    private class StartLink implements OnSuccessListener<AuthResult> {
        private final IdpResponse mResponse;

        public StartLink(IdpResponse response) {
            mResponse = response;
        }

        @Override
        public void onSuccess(AuthResult result) {
            if (mRequestedSignInCredential == null) {
                setResult(Resource.forSuccess(mResponse));
            } else {
                result.getUser()
                        .linkWithCredential(mRequestedSignInCredential)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                // Since we've already signed in, it's too late to
                                // backtrack.
                                setResult(Resource.forSuccess(mResponse));
                            }
                        });
            }
        }
    }
}
