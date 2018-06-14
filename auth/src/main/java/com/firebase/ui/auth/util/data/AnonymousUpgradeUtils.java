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

import java.util.UUID;

/**
 * Utilities to help with Anonymous user upgrade.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnonymousUpgradeUtils {

    public static Task<AuthResult> createUserWithEmailAndPasswordOrLink(@NonNull FirebaseAuth auth,
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

    public static boolean canUpgradeAnonymous(FirebaseAuth auth, FlowParameters flowParameters) {
        return flowParameters.isAnonymousUpgradeEnabled() && auth.getCurrentUser() != null &&
                auth.getCurrentUser().isAnonymous();
    }

    @NonNull
    public static Task<AuthResult> validateCredential(FirebaseApp app, AuthCredential credential) {
        // Create a new FirebaseApp so that the anonymous user state is not lost in our
        // original FirebaseAuth instance.
        String randomName = UUID.randomUUID().toString();
        FirebaseApp scratchApp = FirebaseApp.initializeApp(
                app.getApplicationContext(), app.getOptions(), randomName);
        FirebaseAuth scratchAuth = FirebaseAuth.getInstance(scratchApp);
        return scratchAuth.signInWithCredential(credential);
    }
}
