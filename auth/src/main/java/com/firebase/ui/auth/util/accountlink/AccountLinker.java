package com.firebase.ui.auth.util.accountlink;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
public class AccountLinker implements OnSuccessListener<AuthResult>, OnFailureListener {
    private static final String TAG = "AccountLinker";

    private final HelperActivityBase mActivity;
    private final IdpResponse mIdpResponse;

    /** The credential of the user's existing account. */
    private final AuthCredential mExistingCredential;

    /** The credential the user originally tried to sign in with. */
    @Nullable private final AuthCredential mNewCredential;

    private AccountLinker(HelperActivityBase activity,
                          IdpResponse response,
                          @NonNull AuthCredential existingCredential,
                          @Nullable AuthCredential newCredential) {
        mActivity = activity;
        mIdpResponse = response;
        mExistingCredential = existingCredential;
        mNewCredential = newCredential;

        start();
    }

    public static void linkWithCurrentUser(HelperActivityBase activity,
                                           IdpResponse response,
                                           AuthCredential existingCredential) {
        new AccountLinker(activity, response, existingCredential, null);
    }

    public static void linkToNewUser(HelperActivityBase activity,
                                     IdpResponse response,
                                     AuthCredential existingCredential,
                                     AuthCredential newCredential) {
        new AccountLinker(activity, response, existingCredential, newCredential);
    }

    private void start() {
        FirebaseUser currentUser = mActivity.getAuthHelper().getCurrentUser();
        if (currentUser == null) {
            // The user has an existing account and is trying to log in with a new provider
            mActivity.getAuthHelper().getFirebaseAuth()
                    .signInWithCredential(mExistingCredential)
                    .addOnSuccessListener(this)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            finishWithError();
                        }
                    })
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error signing in with new credential"));
        } else {
            // If the current user is trying to sign in with a new account, just link the two
            // and we should be fine. Otherwise, we are probably working with an anonymous account
            // trying to be linked to an existing account which is bound to fail.
            currentUser
                    .linkWithCredential(mNewCredential == null ? mExistingCredential : mNewCredential)
                    .continueWithTask(new ProfileMerger(mIdpResponse))
                    .addOnSuccessListener(new FinishListener())
                    .addOnFailureListener(this)
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error linking with credential"));
        }
    }

    @Override
    public void onSuccess(AuthResult result) {
        if (mNewCredential == null) {
            mActivity.finish(Activity.RESULT_OK, mIdpResponse.toIntent());
        } else {
            // Link the user's existing account (mExistingCredential) with the account they were
            // trying to sign in to (mNewCredential)
            result.getUser()
                    .linkWithCredential(mNewCredential)
                    .continueWithTask(new ProfileMerger(mIdpResponse))
                    .addOnFailureListener(new TaskFailureLogger(
                            TAG, "Error signing in with previous credential"))
                    .addOnCompleteListener(new FinishListener());
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException
                && mActivity.getAuthHelper().canLinkAccounts()) {
            mIdpResponse.getUser().setPrevUid(mActivity.getAuthHelper().getUidForAccountLinking());

            // Since we still want the user to be able to sign in even though
            // they have an existing account, we are going to save the uid of the
            // current user, log them out, and then sign in with the new credential.
            Task<AuthResult> signInTask = ManualMergeUtils.injectSignInTaskBetweenDataTransfer(mActivity,
                    mIdpResponse,
                    new Callable<Task<AuthResult>>() {
                        @Override
                        public Task<AuthResult> call() throws Exception {
                            return mActivity.getAuthHelper().getFirebaseAuth()
                                    .signInWithCredential(mExistingCredential)
                                    .continueWithTask(new ProfileMerger(mIdpResponse));
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            finishWithError();
                        }
                    })
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error signing in with credential"));

            // Occurs when the user is logged and they are trying to sign in with an existing account.
            if (mNewCredential == null) {
                signInTask.addOnSuccessListener(new FinishListener());
            } else {
                // 3 way account linking!!!
                // Occurs if the user is logged, trying to sign in with a new provider,
                // and already has existing providers.
                signInTask.addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        mActivity.getAuthHelper().getCurrentUser()
                                .linkWithCredential(mNewCredential)
                                .continueWithTask(new ProfileMerger(mIdpResponse))
                                .addOnFailureListener(
                                        new TaskFailureLogger(TAG, "Error linking with credential"))
                                .addOnCompleteListener(new FinishListener());
                    }
                });
            }
        } else {
            Log.w(TAG, "See AuthUI.SignInIntentBuilder#setIsAccountLinkingEnabled(boolean, Class)"
                    + " to support account linking");
            finishWithError();
        }
    }

    private void finishWithError() {
        mActivity.finish(
                Activity.RESULT_CANCELED,
                IdpResponse.getErrorCodeIntent(ErrorCodes.UNKNOWN_ERROR));
    }

    private class FinishListener implements OnCompleteListener<AuthResult>, OnSuccessListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task task) {
            finishOk();
        }

        @Override
        public void onSuccess(AuthResult result) {
            finishOk();
        }

        private void finishOk() {
            mActivity.finish(Activity.RESULT_OK, mIdpResponse.toIntent());
        }
    }
}
