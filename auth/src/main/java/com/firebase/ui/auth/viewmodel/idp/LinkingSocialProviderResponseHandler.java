package com.firebase.ui.auth.viewmodel.idp;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.SignInViewModelBase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LinkingSocialProviderResponseHandler extends SignInViewModelBase {
    private AuthCredential mRequestedSignInCredential;

    public LinkingSocialProviderResponseHandler(Application application) {
        super(application);
    }

    public void setRequestedSignInCredential(@Nullable AuthCredential credential) {
        mRequestedSignInCredential = credential;
    }

    public void startSignIn(@NonNull final IdpResponse response) {
        if (!response.isSuccessful()) {
            setResult(Resource.<IdpResponse>forFailure(response.getError()));
            return;
        }
        if (!AuthUI.SOCIAL_PROVIDERS.contains(response.getProviderType())) {
            throw new IllegalStateException(
                    "This handler cannot be used to link email or phone providers");
        }
        setResult(Resource.<IdpResponse>forLoading());

        AuthCredential credential = ProviderUtils.getAuthCredential(response);
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            getAuth().signInWithCredential(credential)
                    .continueWithTask(new Continuation<AuthResult, Task<AuthResult>>() {
                        @Override
                        public Task<AuthResult> then(@NonNull Task<AuthResult> task) {
                            final AuthResult result = task.getResult();
                            if (mRequestedSignInCredential == null) {
                                return Tasks.forResult(result);
                            } else {
                                return result.getUser()
                                        .linkWithCredential(mRequestedSignInCredential)
                                        .continueWith(new Continuation<AuthResult, AuthResult>() {
                                            @Override
                                            public AuthResult then(@NonNull Task<AuthResult> task) {
                                                if (task.isSuccessful()) {
                                                    return task.getResult();
                                                } else {
                                                    // Since we've already signed in, it's too late
                                                    // to backtrack so we just ignore any errors.
                                                    return result;
                                                }
                                            }
                                        });
                            }
                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                handleSuccess(response, task.getResult());
                            } else {
                                setResult(Resource.<IdpResponse>forFailure(task.getException()));
                            }
                        }
                    });
        } else {
            currentUser.linkWithCredential(credential)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // I'm not sure why we ignore failures here, but this mirrors previous
                            // behavior.
                            handleSuccess(response, task.getResult());
                        }
                    });
        }
    }
}
