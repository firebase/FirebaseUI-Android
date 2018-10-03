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
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.firebase.ui.auth.viewmodel.SignInViewModelBase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SocialProviderResponseHandler extends SignInViewModelBase {
    public SocialProviderResponseHandler(Application application) {
        super(application);
    }

    public void startSignIn(@NonNull final IdpResponse response) {
        if (!response.isSuccessful()) {
            setResult(Resource.<IdpResponse>forFailure(response.getError()));
            return;
        }
        if (!AuthUI.SOCIAL_PROVIDERS.contains(response.getProviderType())) {
            throw new IllegalStateException(
                    "This handler cannot be used with email or phone providers");
        }

        setResult(Resource.<IdpResponse>forLoading());

        final AuthCredential credential = ProviderUtils.getAuthCredential(response);

        AuthOperationManager.getInstance().signInAndLinkWithCredential(
                getAuth(),
                getArguments(),
                credential)
                .continueWithTask(new ProfileMerger(response))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        handleSuccess(response, result);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            final String email = response.getEmail();
                            if (email == null) {
                                setResult(Resource.<IdpResponse>forFailure(e));
                                return;
                            }
                            // There can be a collision due to:
                            // CASE 1: Anon user signing in with a credential that belongs to an
                            // existing user.
                            // CASE 2: non - anon user signing in with a credential that does not
                            // belong to an existing user, but the email matches an existing user
                            // that has another social IDP. We need to link this new IDP to this
                            // existing user.
                            // CASE 3: CASE 2 with an anonymous user. We link the new IDP to the
                            // same account before handling invoking a merge failure.
                            ProviderUtils.fetchSortedProviders(getAuth(), getArguments(), email)
                                    .addOnSuccessListener(new OnSuccessListener<List<String>>() {
                                        @Override
                                        public void onSuccess(List<String> providers) {
                                            if (providers.contains(response.getProviderType())) {
                                                // Case 1
                                                handleMergeFailure(credential);
                                            } else if (providers.isEmpty()) {
                                                setResult(Resource.<IdpResponse>forFailure(
                                                        new FirebaseUiException(
                                                                ErrorCodes.DEVELOPER_ERROR,
                                                                "No supported providers.")));
                                            } else {
                                                // Case 2 & 3 - we need to link
                                                startWelcomeBackFlowForLinking(
                                                        providers.get(0), response);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            setResult(Resource.<IdpResponse>forFailure(
                                                    e));
                                        }
                                    });
                        }
                    }
                });
    }

    public void startWelcomeBackFlowForLinking(String provider, IdpResponse response) {
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
                            response),
                    RequestCodes.ACCOUNT_LINK_FLOW
            )));
        } else {
            // Start Idp welcome back flow
            setResult(Resource.<IdpResponse>forFailure(new IntentRequiredException(
                    WelcomeBackIdpPrompt.createIntent(
                            getApplication(),
                            getArguments(),
                            new User.Builder(provider, response.getEmail()).build(),
                            response),
                    RequestCodes.ACCOUNT_LINK_FLOW
            )));
        }
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

}
