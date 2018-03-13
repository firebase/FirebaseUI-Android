package com.firebase.ui.auth.util;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.google.android.gms.tasks.Task;
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
    public static Task<AuthResult> signInOrLink(FlowParameters flowParameters,
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

    private static boolean canUpgradeAnonymous(FlowParameters parameters, FirebaseAuth auth) {
        return parameters.enableAnonymousUpgrade
                && auth.getCurrentUser() != null
                && auth.getCurrentUser().isAnonymous();
    }

}
