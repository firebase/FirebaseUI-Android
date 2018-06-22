package com.firebase.ui.auth.viewmodel.email;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.ProfileMerger;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.util.data.TaskFailureLogger;
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

/**
 * Handles the logic for {@link com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt} including
 * signing in with email and password, linking other credentials, and saving credentials to
 * SmartLock.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WelcomeBackPasswordHandler extends SignInViewModelBase {
    private static final String TAG = "WBPasswordHandler";

    private String mPendingPassword;

    public WelcomeBackPasswordHandler(Application application) {
        super(application);
    }

    /**
     * Kick off the sign-in process.
     */
    public void startSignIn(@NonNull final String email,
                            @NonNull final String password,
                            @NonNull final IdpResponse inputResponse,
                            @Nullable final AuthCredential credential) {
        setResult(Resource.<IdpResponse>forLoading());

        // Store the password before signing in so it can be used for later credential building
        mPendingPassword = password;

        // Build appropriate IDP response based on inputs
        final IdpResponse outputResponse;
        if (credential == null) {
            // New credential for the email provider
            outputResponse = new IdpResponse.Builder(
                    new User.Builder(EmailAuthProvider.PROVIDER_ID, email).build())
                    .build();
        } else {
            // New credential for an IDP (Phone or Social)
            outputResponse = new IdpResponse.Builder(inputResponse.getUser())
                    .setToken(inputResponse.getIdpToken())
                    .setSecret(inputResponse.getIdpSecret())
                    .build();
        }

        final AuthOperationManager authOperationManager = AuthOperationManager.getInstance();
        if (authOperationManager.canUpgradeAnonymous(getAuth(), getArguments())) {
            final AuthCredential credToValidate = EmailAuthProvider.getCredential(email, password);

            // Check to see if we need to link (for social providers with the same email)
            if (AuthUI.SOCIAL_PROVIDERS.contains(inputResponse.getProviderType())) {
                // Add the provider to the same account before triggering a merge failure.
                authOperationManager.safeLink(credToValidate, credential, getArguments())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult result) {
                                handleMergeFailure(credToValidate);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                setResult(Resource.<IdpResponse>forFailure(e));
                            }
                        });
            } else {
                // The user has not tried to log in with a federated IDP containing the same email.
                // In this case, we just need to verify that the credential they provided is valid.
                // No linking is done for non-federated IDPs.
                // A merge failure occurs because the account exists and the user is anonymous.
                authOperationManager.validateCredential(credToValidate, getArguments())
                        .addOnCompleteListener(
                                new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            handleMergeFailure(credToValidate);
                                        } else {
                                            setResult(Resource.<IdpResponse>forFailure(task.getException()));
                                        }
                                    }
                                });
            }
        } else {
            // Kick off the flow including signing in, linking accounts, and saving with SmartLock
            getAuth().signInWithEmailAndPassword(email, password)
                    .continueWithTask(new Continuation<AuthResult, Task<AuthResult>>() {
                        @Override
                        public Task<AuthResult> then(@NonNull Task<AuthResult> task) throws Exception {
                            // Forward task failure by asking for result
                            AuthResult result = task.getResult(Exception.class);

                            // Task succeeded, link user if necessary
                            if (credential == null) {
                                return Tasks.forResult(result);
                            } else {
                                return result.getUser()
                                        .linkWithCredential(credential)
                                        .continueWithTask(new ProfileMerger(outputResponse))
                                        .addOnFailureListener(new TaskFailureLogger(TAG,
                                                "linkWithCredential+merge failed."));
                            }
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult result) {
                            handleSuccess(outputResponse, result);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            setResult(Resource.<IdpResponse>forFailure(e));
                        }
                    })
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "signInWithEmailAndPassword failed."));
        }
    }

    /**
     * Get the most recent pending password.
     */
    public String getPendingPassword() {
        return mPendingPassword;
    }
}
