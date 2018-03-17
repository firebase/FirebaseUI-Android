package com.firebase.ui.auth.util.accountlink;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.remote.ProfileMerger;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.data.TaskFailureLogger;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Callable;

/**
 * "One link to rule them all." - AccountLinker
 * <p><p>
 * AccountLinker can handle up to 3 way account linking: user is currently logged in anonymously,
 * has an existing Google account, and is trying to log in with Facebook. Results: Google and
 * Facebook are linked and the uid of the anonymous account is returned for manual merging.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AccountLinker {
    private static final String TAG = "AccountLinker";

    private final HelperActivityBase mActivity;
    private final IdpResponse mIdpResponse;

    /** The credential of the user's existing account. */
    private final AuthCredential mExistingCredential;

    /** The credential the user originally tried to sign in with. */
    @Nullable private final AuthCredential mNewCredential;

    private final Task<AuthResult> mTask;

    private AccountLinker(HelperActivityBase activity,
                          IdpResponse response,
                          @NonNull AuthCredential existingCredential,
                          @Nullable AuthCredential newCredential) {
        mActivity = activity;
        mIdpResponse = response;
        mExistingCredential = existingCredential;
        mNewCredential = newCredential;

        mTask = start();
    }

    public static Task<AuthResult> linkWithCurrentUser(HelperActivityBase activity,
                                                       IdpResponse response,
                                                       AuthCredential existingCredential) {
        return new AccountLinker(activity, response, existingCredential, null).mTask;
    }

    public static Task<AuthResult> linkToNewUser(HelperActivityBase activity,
                                                 IdpResponse response,
                                                 AuthCredential existingCredential,
                                                 AuthCredential newCredential) {
        return new AccountLinker(activity, response, existingCredential, newCredential).mTask;
    }

    private Task<AuthResult> start() {
        FirebaseUser currentUser = mActivity.getAuthHelper().getCurrentUser();
        if (currentUser == null) {
            // The user has an existing account and is trying to log in with a new provider
            return mActivity.getAuthHelper().getFirebaseAuth()
                    .signInWithCredential(mExistingCredential)
                    .continueWithTask(new NoCurrentUser())
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error signing in with new credential"));
        } else {
            // If the current user is trying to sign in with a new account, just link the two
            // and we should be fine. Otherwise, we are probably working with an anonymous account
            // trying to be linked to an existing account which is bound to fail.
            return currentUser
                    .linkWithCredential(mNewCredential == null ? mExistingCredential : mNewCredential)
                    .continueWithTask(new ProfileMerger(mIdpResponse))
                    .continueWithTask(new ExistingUser())
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error linking with credential"));
        }
    }

    private final class NoCurrentUser implements Continuation<AuthResult, Task<AuthResult>> {
        @Override
        public Task<AuthResult> then(@NonNull Task<AuthResult> task) {
            if (mNewCredential == null) {
                return task;
            } else {
                // Link the user's existing account (mExistingCredential) with the account they were
                // trying to sign in to (mNewCredential)
                return task.getResult().getUser()
                        .linkWithCredential(mNewCredential)
                        .continueWithTask(new ProfileMerger(mIdpResponse))
                        .addOnFailureListener(new TaskFailureLogger(
                                TAG, "Error signing in with previous credential"));
            }
        }
    }

    private final class ExistingUser implements Continuation<AuthResult, Task<AuthResult>> {
        @Override
        public Task<AuthResult> then(@NonNull final Task<AuthResult> task) {
            if (task.getException() instanceof FirebaseAuthUserCollisionException
                    && mActivity.getAuthHelper().canLinkAccounts()) {
                mIdpResponse.getUser()
                        .setPrevUid(mActivity.getAuthHelper().getUidForAccountLinking());

                // Since we still want the user to be able to sign in even though
                // they have an existing account, we are going to save the uid of the
                // current user, log them out, and then sign in with the new credential.
                Task<AuthResult> signInTask = ManualMergeUtils.injectSignInTaskBetweenDataTransfer(
                        mActivity,
                        mIdpResponse,
                        mActivity.getFlowParams(),
                        new Callable<Task<AuthResult>>() {
                            @Override
                            public Task<AuthResult> call() {
                                return mActivity.getAuthHelper().getFirebaseAuth()
                                        .signInWithCredential(mExistingCredential)
                                        .continueWithTask(new ProfileMerger(mIdpResponse))
                                        .continueWithTask(new ExceptionWrapper(task));
                            }
                        })
                        .addOnFailureListener(
                                new TaskFailureLogger(TAG, "Error signing in with credential"));

                // Occurs when the user is logged and they are trying to sign in with an existing account.
                if (mNewCredential == null) {
                    return signInTask;
                } else {
                    // 3 way account linking!!!
                    // Occurs if the user is logged, trying to sign in with a new provider,
                    // and already has existing providers.
                    return signInTask.continueWithTask(new Continuation<AuthResult, Task<AuthResult>>() {
                        @Override
                        public Task<AuthResult> then(@NonNull Task<AuthResult> task) {
                            return mActivity.getAuthHelper().getCurrentUser()
                                    .linkWithCredential(mNewCredential)
                                    .continueWithTask(new ProfileMerger(mIdpResponse))
                                    .continueWithTask(new ExceptionWrapper(task))
                                    .addOnFailureListener(
                                            new TaskFailureLogger(TAG,
                                                    "Error linking with credential"));
                        }
                    });
                }
            } else {
                return task;
            }
        }

        private final class ExceptionWrapper implements Continuation<AuthResult, Task<AuthResult>> {
            private final Task<AuthResult> mWrapped;

            public ExceptionWrapper(Task<AuthResult> wrapped) {
                mWrapped = wrapped;
            }

            @Override
            public Task<AuthResult> then(@NonNull Task<AuthResult> task) {
                try {
                    task.getResult(Exception.class);
                    return task;
                } catch (Exception e) {
                    return Tasks.forException((Exception) e.initCause(mWrapped.getException()));
                }
            }
        }
    }
}
