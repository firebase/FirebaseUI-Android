package com.firebase.ui.auth.viewmodel.idp;

import android.app.Application;
import android.text.TextUtils;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.SignInViewModelBase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LinkingSocialProviderResponseHandler extends SignInViewModelBase {
    private AuthCredential mRequestedSignInCredential;
    private String mEmail;

    public LinkingSocialProviderResponseHandler(Application application) {
        super(application);
    }

    public void setRequestedSignInCredentialForEmail(@Nullable AuthCredential credential,
                                                     @Nullable String email) {
        mRequestedSignInCredential = credential;
        mEmail = email;
    }

    public void startSignIn(@NonNull final IdpResponse response) {
        if (!response.isSuccessful()) {
            setResult(Resource.<IdpResponse>forFailure(response.getError()));
            return;
        }
        if (isInvalidProvider(response.getProviderType())) {
            throw new IllegalStateException(
                    "This handler cannot be used to link email or phone providers.");
        }
        if (mEmail != null && !mEmail.equals(response.getEmail())) {
            setResult(Resource.<IdpResponse>forFailure(new FirebaseUiException
                    (ErrorCodes.EMAIL_MISMATCH_ERROR)));
            return;
        }

        setResult(Resource.<IdpResponse>forLoading());

        // The Generic IDP flow does not return a credential - it signs us in right away.
        // If the user was prompted to sign-in via Generic IDP, we can link immediately.
        // Example: Existing user with Yahoo provider - signs in with microsoft -
        // prompted to sign in with yahoo. Sign in with Yahoo will be succesful, it won't
        // return a credential.
        if (isGenericIdpLinkingFlow(response.getProviderType())) {
            getAuth().getCurrentUser()
                    .linkWithCredential(mRequestedSignInCredential)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            handleSuccess(response, authResult);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Resource.<IdpResponse>forFailure(e);
                        }
                    });
            return;
        }

        final AuthOperationManager authOperationManager = AuthOperationManager.getInstance();
        final AuthCredential credential = ProviderUtils.getAuthCredential(response);

        if (authOperationManager.canUpgradeAnonymous(getAuth(), getArguments())) {
            if (mRequestedSignInCredential == null) {
                // The user has provided a valid credential by signing in with a federated
                // idp. linkWithCredential will fail because the user is anonymous and the account
                // exists (we're in the welcome back flow).
                // We know that they are signing in with the same IDP because requestSignInCredential
                // is null.
                // We just need to have the developer handle the merge failure.
                handleMergeFailure(credential);
            } else {
                // The user has logged in with an IDP that has the same email with another IDP
                // present on the account.
                // These IDPs belong to the same account - they must be linked, but we can't lose
                // our anonymous user session
                authOperationManager.safeLink(credential, mRequestedSignInCredential, getArguments())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult result) {
                                handleMergeFailure(credential);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                setResult(Resource.<IdpResponse>forFailure(e));
                            }
                        });
            }
        } else {
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
        }
    }

    public boolean hasCredentialForLinking() {
        return mRequestedSignInCredential != null;
    }

    private boolean isGenericIdpLinkingFlow(@NonNull String providerId) {
        // TODO(lsirac): Remove use of SUPPORTED_OAUTH_PROVIDERS when we decide to support all IDPs
        return AuthUI.SUPPORTED_OAUTH_PROVIDERS.contains(providerId)
                && mRequestedSignInCredential != null
                && getAuth().getCurrentUser() != null
                && !getAuth().getCurrentUser().isAnonymous();
    }

    private boolean isInvalidProvider(@NonNull String provider) {
        return TextUtils.equals(provider, EmailAuthProvider.PROVIDER_ID)
                || TextUtils.equals(provider, PhoneAuthProvider.PROVIDER_ID);
    }
}
