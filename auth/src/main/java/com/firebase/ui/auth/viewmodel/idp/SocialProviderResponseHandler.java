package com.firebase.ui.auth.viewmodel.idp;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.IntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.ProfileMerger;
import com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt;
import com.firebase.ui.auth.ui.idp.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.util.accountlink.AccountLinker;
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
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;

import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SocialProviderResponseHandler extends AuthViewModelBase<IdpResponse> {
    public SocialProviderResponseHandler(Application application) {
        super(application);
    }

    public void startSignIn(@NonNull final IdpResponse response) {
        if (!response.isSuccessful()) {
            setResult(Resource.<IdpResponse>forFailure(response.getError()));
            return;
        }
        if (response.getProviderType().equals(EmailAuthProvider.PROVIDER_ID)
                || response.getProviderType().equals(PhoneAuthProvider.PROVIDER_ID)) {
            setResult(Resource.forSuccess(response));
            return;
        }
        setResult(Resource.<IdpResponse>forLoading());

        AuthCredential credential = ProviderUtils.getAuthCredential(response);
        Task<AuthResult> signInTask;
        if (canLinkAccounts()) {
            signInTask = getCurrentUser()
                    .linkWithCredential(credential)
                    .continueWithTask(new ProfileMerger(response));
        } else {
            signInTask = getAuth().signInWithCredential(credential);
        }

        signInTask.continueWithTask(new ProfileMerger(response))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        setResult(Resource.forSuccess(response));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String email = response.getEmail();
                        if (email != null) {
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                getAuth().fetchProvidersForEmail(email)
                                        .addOnSuccessListener(new StartWelcomeBackFlow(response))
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                setResult(Resource.<IdpResponse>forFailure(e));
                                            }
                                        });
                                return;
                            }
                        }
                        setResult(Resource.<IdpResponse>forFailure(e));
                    }
                });
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

    private class StartWelcomeBackFlow implements OnSuccessListener<ProviderQueryResult> {
        private final IdpResponse mResponse;

        public StartWelcomeBackFlow(IdpResponse response) {
            mResponse = response;
        }

        @Override
        public void onSuccess(ProviderQueryResult result) {
            List<String> providers = result.getProviders();
            AuthCredential credential = ProviderUtils.getAuthCredential(mResponse);
            if (canLinkAccounts() && credential != null
                    && providers != null && providers.contains(credential.getProvider())) {
                // We don't want to show the welcome back dialog since the user selected
                // an existing account and we can just link the two accounts without knowing
                // prevCredential.
                AccountLinker.linkWithCurrentUser(
                        SocialProviderResponseHandler.this, mResponse, credential)
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult result) {
                                setResult(Resource.forSuccess(mResponse));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                setResult(Resource.<IdpResponse>forFailure(e));
                            }
                        });
                return;
            }

            @AuthUI.SupportedProvider String provider = ProviderUtils.getLastUsedProvider(result);
            if (provider == null) {
                throw new IllegalStateException(
                        "No provider even though we received a FirebaseAuthUserCollisionException");
            } else if (provider.equals(EmailAuthProvider.PROVIDER_ID)) {
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
                                new User.Builder(provider, mResponse.getEmail()).build(),
                                mResponse),
                        RequestCodes.ACCOUNT_LINK_FLOW
                )));
            }
        }
    }
}
