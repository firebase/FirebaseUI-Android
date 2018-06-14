package com.firebase.ui.auth.viewmodel.email;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.ProfileMerger;
import com.firebase.ui.auth.util.data.AnonymousUpgradeUtils;
import com.firebase.ui.auth.util.data.TaskFailureLogger;
import com.firebase.ui.auth.viewmodel.SignInViewModelBase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
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

        // We first check if we are trying to upgrade an anonymous user
        // Calling linkWithCredential will fail, so we can save an RPC call
        // We just need to validate the credential
        if (AnonymousUpgradeUtils.canUpgradeAnonymous(getAuth(), getArguments())) {
            final AuthCredential credToValidate = EmailAuthProvider.getCredential(email, password);

            AnonymousUpgradeUtils.validateCredential(FirebaseApp.getInstance(), credToValidate)
                    .continueWith(new Continuation<AuthResult, Void>() {
                        @Override
                        public Void then(@NonNull Task<AuthResult> task) throws Exception {
                            if (task.isSuccessful()) {
                                IdpResponse mergeResponse = new IdpResponse.Builder(credToValidate)
                                        .build();
                                setResult(Resource.<IdpResponse>forFailure(new FirebaseAuthAnonymousUpgradeException(
                                        ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT,
                                        mergeResponse)));
                            } else {
                                setResult(Resource.<IdpResponse>forFailure(task.getException()));
                            }
                            return null;
                        }
                    });
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
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                setResult(Resource.<IdpResponse>forFailure(task.getException()));
                                return;
                            }

                            handleSuccess(outputResponse, task.getResult());
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
