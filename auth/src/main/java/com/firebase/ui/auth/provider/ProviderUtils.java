/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.firebase.ui.auth.provider;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.List;

public final class ProviderUtils {
    private static final String TAG = "ProviderUtils";

    private ProviderUtils() {
        throw new AssertionError("No instance for you!");
    }

    @Nullable
    public static AuthCredential getAuthCredential(IdpResponse response) {
        switch (response.getProviderType()) {
            case GoogleAuthProvider.PROVIDER_ID:
                return GoogleAuthProvider.getCredential(response.getIdpToken(), null);
            case FacebookAuthProvider.PROVIDER_ID:
                return FacebookAuthProvider.getCredential(response.getIdpToken());
            case TwitterAuthProvider.PROVIDER_ID:
                return TwitterAuthProvider.getCredential(response.getIdpToken(),
                        response.getIdpSecret());
            default:
                return null;
        }
    }

    /**
     * Translate a Firebase Auth provider ID (such as {@link GoogleAuthProvider#PROVIDER_ID}) to a
     * Credentials API account type (such as {@link IdentityProviders#GOOGLE}).
     */
    public static String providerIdToAccountType(@AuthUI.SupportedProvider @NonNull String providerId) {
        switch (providerId) {
            case GoogleAuthProvider.PROVIDER_ID:
                return IdentityProviders.GOOGLE;
            case FacebookAuthProvider.PROVIDER_ID:
                return IdentityProviders.FACEBOOK;
            case TwitterAuthProvider.PROVIDER_ID:
                return IdentityProviders.TWITTER;
            case EmailAuthProvider.PROVIDER_ID:
                // The account type for email/password creds is null
            case PhoneAuthProvider.PROVIDER_ID:
                // The account type for phone creds is null
            default:
                return null;
        }
    }

    public static Task<String> fetchTopProvider(FirebaseAuth auth, @NonNull String email) {
        if (TextUtils.isEmpty(email)) {
            return Tasks.forException(new NullPointerException("Email cannot be empty"));
        }

        return auth.fetchProvidersForEmail(email)
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Error fetching providers for email"))
                .continueWith(new Continuation<ProviderQueryResult, String>() {
                    @Override
                    public String then(@NonNull Task<ProviderQueryResult> task) {
                        if (!task.isSuccessful()) return null;

                        List<String> providers = task.getResult().getProviders();
                        return providers == null || providers.isEmpty()
                                ? null : providers.get(providers.size() - 1);
                    }
                });
    }
}
