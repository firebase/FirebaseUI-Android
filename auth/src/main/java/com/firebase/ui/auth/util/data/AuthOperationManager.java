/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.util.data;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.OAuthProvider;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

/**
 * Utilities to help with Anonymous user upgrade.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AuthOperationManager {

    private static String firebaseAppName = "FUIScratchApp";

    private static AuthOperationManager mAuthManager;

    @VisibleForTesting
    public FirebaseAuth mScratchAuth;

    private AuthOperationManager() {}

    public static synchronized AuthOperationManager getInstance() {
        if (mAuthManager == null) {
            mAuthManager = new AuthOperationManager();
        }
        return mAuthManager;
    }

    private FirebaseApp getScratchApp(FirebaseApp defaultApp) {
        try {
            return FirebaseApp.getInstance(firebaseAppName);
        } catch (IllegalStateException e) {
            return FirebaseApp.initializeApp(defaultApp.getApplicationContext(),
                    defaultApp.getOptions(), firebaseAppName);
        }
    }

    private FirebaseAuth getScratchAuth(FlowParameters flowParameters) {
        // Use a different FirebaseApp so that the anonymous user state is not lost in our
        // original FirebaseAuth instance.
        if (mScratchAuth == null) {
            AuthUI authUI = AuthUI.getInstance(flowParameters.appName);
            mScratchAuth = FirebaseAuth.getInstance(getScratchApp(authUI.getApp()));

            if (authUI.isUseEmulator()) {
                mScratchAuth.useEmulator(authUI.getEmulatorHost(), authUI.getEmulatorPort());
            }
        }
        return mScratchAuth;
    }

    public Task<AuthResult> createOrLinkUserWithEmailAndPassword(@NonNull FirebaseAuth auth,
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

    public Task<AuthResult> signInAndLinkWithCredential(@NonNull FirebaseAuth auth,
                                                        @NonNull FlowParameters flowParameters,
                                                        @NonNull AuthCredential credential) {
        if (canUpgradeAnonymous(auth, flowParameters)) {
            return auth.getCurrentUser().linkWithCredential(credential);
        } else {
            return auth.signInWithCredential(credential);
        }
    }

    public boolean canUpgradeAnonymous(FirebaseAuth auth, FlowParameters flowParameters) {
        return flowParameters.isAnonymousUpgradeEnabled() && auth.getCurrentUser() != null &&
                auth.getCurrentUser().isAnonymous();
    }

    @NonNull
    public Task<AuthResult> validateCredential(AuthCredential credential,
                                               FlowParameters flowParameters) {
        return getScratchAuth(flowParameters).signInWithCredential(credential);
    }

    public Task<AuthResult> safeLink(final AuthCredential credential,
                                     final AuthCredential credentialToLink,
                                     final FlowParameters flowParameters) {
        return getScratchAuth(flowParameters)
                .signInWithCredential(credential)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().getUser().linkWithCredential(credentialToLink);
                    }
                    return task;
                });
    }

    @NonNull
    public Task<AuthResult> safeGenericIdpSignIn(@NonNull final HelperActivityBase activity,
                                                 @NonNull final OAuthProvider provider,
                                                 @NonNull final FlowParameters flowParameters) {
        return getScratchAuth(flowParameters)
                .startActivityForSignInWithProvider(activity, provider);
    }
}
