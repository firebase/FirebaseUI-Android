package com.firebase.ui.auth.viewmodel.idp;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.IntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.ProfileMerger;
import com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt;
import com.firebase.ui.auth.ui.idp.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
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

    public void startSignIn(@NonNull IdpResponse inputResponse) {
        if (checkInvalidStateForSignIn(inputResponse)) return;
        startSignIn(ProviderUtils.getAuthCredential(inputResponse), inputResponse);
    }

    /** Kick off the sign-in process. */
    public void startSignIn(@NonNull AuthCredential credential,
                            @NonNull final IdpResponse inputResponse) {
        if (checkInvalidStateForSignIn(inputResponse)) return;
        setResult(Resource.<IdpResponse>forLoading());

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
                        String email = inputResponse.getEmail();
                        if (email != null) {
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                ProviderUtils.fetchTopProvider(getAuth(), email)
                                        .addOnSuccessListener(
                                                new StartWelcomeBackFlow(inputResponse))
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                setResult(Resource.<IdpResponse>forFailure(e));
                                            }
                                        });
                            }
                        } else {
                            setResult(Resource.<IdpResponse>forFailure(e));
                        }
                    }
                });
    }

    private boolean checkInvalidStateForSignIn(@NonNull IdpResponse inputResponse) {
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

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RequestCodes.ACCOUNT_LINK_FLOW) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == Activity.RESULT_OK) {
                setResult(Resource.forSuccess(response));
            } else {
                Exception e;
                if (response == null) {
                    e = new FirebaseUiException(
                            ErrorCodes.UNKNOWN_ERROR, "Link canceled by user.");
                } else {
                    e = response.getError();
                }
                setResult(Resource.<IdpResponse>forFailure(e));
            }
        }
    }

    private class StartWelcomeBackFlow implements OnSuccessListener<String> {
        private final IdpResponse mResponse;

        public StartWelcomeBackFlow(IdpResponse response) {
            mResponse = response;
        }

        @Override
        public void onSuccess(String provider) {
            if (provider == null) {
                throw new IllegalStateException(
                        "No provider even though we received a FirebaseAuthUserCollisionException");
            }

            if (provider.equals(EmailAuthProvider.PROVIDER_ID)) {
                // Start email welcome back flow
                setResult(Resource.<IdpResponse>forFailure(new IntentRequiredException(
                        WelcomeBackPasswordPrompt.createIntent(
                                getApplication(),
                                getArguments(),
                                mResponse),
                        RequestCodes.ACCOUNT_LINK_FLOW
                )));
            } else {
                // Start Idp welcome back flow
                setResult(Resource.<IdpResponse>forFailure(new IntentRequiredException(
                        WelcomeBackIdpPrompt.createIntent(
                                getApplication(),
                                getArguments(),
                                new User.Builder(provider, mResponse.getEmail()).build()),
                        RequestCodes.ACCOUNT_LINK_FLOW
                )));
            }
        }
    }
}

