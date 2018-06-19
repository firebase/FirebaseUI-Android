package com.firebase.ui.auth.util.data;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Utilities to help with Anonymous user upgrade.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AuthOperationManager {

    private static String firebaseAppName = "FUIScratchApp";

    private static synchronized FirebaseApp getScratchApp(FirebaseApp defaultApp) {
        try {
            return FirebaseApp.getInstance(firebaseAppName);
        } catch (IllegalStateException e) {
            return FirebaseApp.initializeApp(defaultApp.getApplicationContext(),
                    defaultApp.getOptions(), firebaseAppName);
        }
    }

    public static FirebaseAuth getScratchAuth(FlowParameters flowParameters) {
        // Use a different FirebaseApp so that the anonymous user state is not lost in our
        // original FirebaseAuth instance.
        FirebaseApp app = FirebaseApp.getInstance(flowParameters.appName);
        return FirebaseAuth.getInstance(getScratchApp(app));
    }

    public static Task<AuthResult> createOrLinkUserWithEmailAndPassword(@NonNull FirebaseAuth auth,
                                                                        @NonNull FlowParameters flowParameters,
                                                                        @NonNull String email,
                                                                        @NonNull String password) {
        if (canUpgradeAnonymous(auth, flowParameters)) {
            AuthCredential credential = EmailAuthProvider.getCredential(email, password);
            return auth.getCurrentUser().linkWithCredential(credential);
        } else {
            return auth.createUserWithEmailAndPassword(email, password);
        }
    }

    public static Task<AuthResult> signInAndLinkWithCredential(@NonNull FirebaseAuth auth,
                                                               @NonNull FlowParameters flowParameters,
                                                               @NonNull AuthCredential credential) {
        if (canUpgradeAnonymous(auth, flowParameters)) {
            return auth.getCurrentUser().linkWithCredential(credential);
        } else {
            return auth.signInWithCredential(credential);
        }
    }

    public static boolean canUpgradeAnonymous(FirebaseAuth auth, FlowParameters flowParameters) {
        return flowParameters.isAnonymousUpgradeEnabled() && auth.getCurrentUser() != null &&
                auth.getCurrentUser().isAnonymous();
    }

    @NonNull
    public static Task<AuthResult> validateCredential(AuthCredential credential,
                                                      FlowParameters flowParameters) {
        return getScratchAuth(flowParameters).signInWithCredential(credential);
    }

    public static Task<AuthResult> safeLink(final AuthCredential credential,
                                            final AuthCredential credentialToLink,
                                            final FlowParameters flowParameters) {


        return getScratchAuth(flowParameters)
                .signInWithCredential(credential)
                .continueWithTask(new Continuation<AuthResult, Task<AuthResult>>() {
                    @Override
                    public Task<AuthResult> then(@NonNull Task<AuthResult> task) throws Exception {
                        if (task.isSuccessful()) {
                            return AuthOperationManager.getScratchAuth(flowParameters)
                                    .getCurrentUser()
                                    .linkWithCredential(credentialToLink);
                        }
                        return task;
                    }
                });
    }
}
