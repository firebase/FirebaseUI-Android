package com.firebase.ui.auth.data.remote;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.CyclicAccountLinkingException;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt;
import com.firebase.ui.auth.ui.idp.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class SignInHandler extends AuthViewModelBase {
    private static final int RC_ACCOUNT_LINK = 11;

    public void signIn(IdpResponse response, @Nullable PhoneAuthCredential credential) {
        if (response.isSuccessful()) {
            Task<AuthResult> base;
            switch (response.getProviderType()) {
                case EmailAuthProvider.PROVIDER_ID:
                    base = handleEmail(response);
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    if (credential == null) {
                        throw new IllegalStateException(
                                "Phone credential cannot be null when signing in with phone provider.");
                    }

                    base = handlePhone(credential);
                    break;
                default:
                    base = handleIdp(response);
            }
            base.continueWithTask(new ProfileMerger(response))
                    .continueWithTask(new SaveCredentialFlow(response))
                    .addOnSuccessListener(new OnSuccessListener<IdpResponse>() {
                        @Override
                        public void onSuccess(IdpResponse response) {
                            SIGN_IN_LISTENER.setValue(response);
                        }
                    })
                    .addOnFailureListener(new FailureFlow(response));
        } else {
            SIGN_IN_LISTENER.setValue(response);
        }
    }

    private Task<AuthResult> handleEmail(final IdpResponse response) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return ProviderUtils.fetchTopProvider(mAuth, response.getEmail())
                    .continueWithTask(new Continuation<String, Task<AuthResult>>() {
                        @Override
                        public Task<AuthResult> then(@NonNull Task<String> task) {
                            if (TextUtils.isEmpty(task.getResult())) {
                                return mAuth.createUserWithEmailAndPassword(
                                        response.getEmail(), response.getPassword());
                            } else {
                                return mAuth.signInWithEmailAndPassword(
                                        response.getEmail(), response.getPassword());
                            }
                        }
                    });
        } else {
            return currentUser.linkWithCredential(EmailAuthProvider.getCredential(
                    response.getEmail(), response.getPassword()));
        }
    }

    private Task<AuthResult> handlePhone(PhoneAuthCredential credential) {
        return handleCredential(credential);
    }

    private class FailureFlow implements OnFailureListener, OnSuccessListener<String>,
            Observer<ActivityResult> {
        private final IdpResponse mResponse;

        public FailureFlow(IdpResponse response) {
            mResponse = response;
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            String email = mResponse.getEmail();
            if (email != null) {
                if (e instanceof FirebaseAuthUserCollisionException) {
                    ProviderUtils.fetchTopProvider(mAuth, email)
                            .addOnSuccessListener(this)
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    SIGN_IN_LISTENER.setValue(IdpResponse.fromError(e));
                                }
                            });
                    return;
                } else if (e instanceof FirebaseAuthInvalidUserException
                        || e instanceof FirebaseAuthInvalidCredentialsException) {
                    deleteCredential(email);
                }
            }

            SIGN_IN_LISTENER.setValue(IdpResponse.fromError(e));
        }

        @Override
        public void onSuccess(String provider) {
            if (((SingleLiveEvent<ActivityResult>) mFlowHolder.getActivityResultListener())
                    .isObserving(getClass())) {
                SIGN_IN_LISTENER.setValue(IdpResponse.fromError(new CyclicAccountLinkingException(
                        "Attempting 3 way linking")));
                return;
            } else {
                mFlowHolder.getActivityResultListener().observeForever(this);
            }

            Toast.makeText(getApplication(), R.string.fui_error_user_collision, Toast.LENGTH_LONG)
                    .show();

            if (provider == null) {
                throw new IllegalStateException(
                        "No provider even though we received a FirebaseAuthUserCollisionException");
            }

            User newUser = new User.Builder(provider, mResponse.getEmail()).build();
            if (provider.equals(EmailAuthProvider.PROVIDER_ID)) {
                // Start email welcome back flow
                mFlowHolder.getIntentStarter().setValue(Pair.create(
                        WelcomeBackPasswordPrompt.createIntent(
                                getApplication(),
                                mFlowHolder.getParams(),
                                newUser),
                        RC_ACCOUNT_LINK));
            } else {
                // Start Idp welcome back flow
                mFlowHolder.getIntentStarter().setValue(Pair.create(
                        WelcomeBackIdpPrompt.createIntent(
                                getApplication(),
                                mFlowHolder.getParams(),
                                newUser),
                        RC_ACCOUNT_LINK));
            }
        }

        @Override
        public void onChanged(@Nullable ActivityResult result) {
            if (result.getRequestCode() != RC_ACCOUNT_LINK) { return; }

            IdpResponse response = IdpResponse.fromResultIntent(result.getData());
            if (result.getResultCode() == Activity.RESULT_OK) {
                onExistingCredentialRetrieved();
            } else if (response != null) {
                onExistingCredentialRetrievalFailure(response);
            }

            mFlowHolder.getActivityResultListener().removeObserver(this);
        }

        /**
         * Called when the user just logged in with their existing account. Now, we can proceed to
         * linking that existing account with the new response we got originally.
         */
        private void onExistingCredentialRetrieved() {
            mAuth.getCurrentUser()
                    .linkWithCredential(ProviderUtils.getAuthCredential(mResponse))
                    .continueWithTask(new ProfileMerger(mResponse))
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // If linking the user's existing account with the new one fails,
                            // we just give up because the user's already signed-in anyway.
                            SIGN_IN_LISTENER.setValue(mResponse);
                        }
                    });
        }

        /**
         * Called when logging in with the existing user fails. This could be because of an
         * attempted data loss link or because of some other unpredictable failure.
         */
        private void onExistingCredentialRetrievalFailure(IdpResponse existingUserResponse) {
            Exception e = existingUserResponse.getException();
            if (e instanceof CyclicAccountLinkingException) {
                // For now, just fail. In the future, we can support a data migration where the
                // anonymous account is thrown away and the other two are linked
                SIGN_IN_LISTENER.setValue(IdpResponse.fromError(e));
            } else {
                SIGN_IN_LISTENER.setValue(IdpResponse.fromError(e));
            }
        }
    }
}
