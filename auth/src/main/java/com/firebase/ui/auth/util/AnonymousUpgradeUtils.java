package com.firebase.ui.auth.util;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

/**
 * Utilities to help with Anonymous user upgrade.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnonymousUpgradeUtils {

    /**
     * Uses type system to enforce the proper failure listener.
     */
    public static class UpgradeTaskWrapper<T> {

        private final Task<T> mWrapped;

        public UpgradeTaskWrapper(Task<T> wrapped) {
            mWrapped = wrapped;
        }

        public Task<T> addOnFailureListener(Activity activity, UpgradeFailureListener listener) {
            return mWrapped.addOnFailureListener(activity, listener);
        }

    }

    @NonNull
    public static Task<AuthResult> signUpOrLink(FlowParameters flowParameters,
                                                FirebaseAuth auth,
                                                String email,
                                                String password) {
        if (canUpgradeAnonymous(flowParameters, auth)) {
            return auth.getCurrentUser()
                    .linkWithCredential(EmailAuthProvider.getCredential(email, password));
        } else {
            return auth.createUserWithEmailAndPassword(email, password);
        }
    }

    @NonNull
    public static UpgradeTaskWrapper<AuthResult> signInOrLink(HelperActivityBase activity,
                                                AuthCredential credential) {
        return new UpgradeTaskWrapper<>(
                signInOrLink(activity.getFlowParams(), activity.getFirebaseAuth(), credential));
    }

    @NonNull
    private static Task<AuthResult> signInOrLink(FlowParameters flowParameters,
                                                FirebaseAuth auth,
                                                AuthCredential credential) {
        if (canUpgradeAnonymous(flowParameters, auth)) {
            return auth.getCurrentUser().linkWithCredential(credential);
        } else {
            return auth.signInWithCredential(credential);
        }
    }

    public static boolean isUpgradeFailure(FlowParameters parameters,
                                           FirebaseAuth auth,
                                           Exception e) {
        return (e instanceof FirebaseAuthUserCollisionException)
                && canUpgradeAnonymous(parameters, auth);
    }

    @NonNull
    public static Task<Void> validateCredential(FirebaseApp app, AuthCredential credential) {
        // Do this operation in an Auth sandbox
        FirebaseAuth sandboxAuth = FirebaseAuth.getInstance(getAppSandbox(app));

        return sandboxAuth.signInWithCredential(credential)
                .continueWith(new Continuation<AuthResult, Void>() {
                    @Override
                    public Void then(@NonNull Task<AuthResult> task) throws Exception {
                        if (task.isSuccessful()) {
                            return null;
                        } else {
                            throw task.getException();
                        }
                    }
                });
    }

    private static FirebaseApp getAppSandbox(FirebaseApp baseApp) {
        String sandboxName = baseApp.getName() + "_sandbox";

        // Get or initialize the sandbox app
        try {
            return FirebaseApp.getInstance(sandboxName);
        } catch (IllegalStateException e) {
            return FirebaseApp.initializeApp(
                    baseApp.getApplicationContext(),
                    baseApp.getOptions(),
                    sandboxName);
        }
    }

    public static boolean canUpgradeAnonymous(FlowParameters parameters, FirebaseAuth auth) {
        return parameters.enableAnonymousUpgrade
                && auth.getCurrentUser() != null
                && auth.getCurrentUser().isAnonymous();
    }
}
