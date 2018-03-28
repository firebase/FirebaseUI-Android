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

package com.firebase.ui.auth.util.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
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
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ProviderUtils {
    private static final String PHONE_IDENTITY = "https://phone.firebase";

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

    @Nullable
    public static String idpResponseToAccountType(@Nullable IdpResponse response) {
        if (response == null) {
            return null;
        }

        return providerIdToAccountType(response.getProviderType());
    }

    @NonNull
    @AuthUI.SupportedProvider
    public static String signInMethodToProviderId(@NonNull String method) {
        switch (method) {
            case GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD:
                return GoogleAuthProvider.PROVIDER_ID;
            case FacebookAuthProvider.FACEBOOK_SIGN_IN_METHOD:
                return FacebookAuthProvider.PROVIDER_ID;
            case TwitterAuthProvider.TWITTER_SIGN_IN_METHOD:
                return TwitterAuthProvider.PROVIDER_ID;
            case PhoneAuthProvider.PHONE_SIGN_IN_METHOD:
                return PhoneAuthProvider.PROVIDER_ID;
            case EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD:
            case EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD:
                return EmailAuthProvider.PROVIDER_ID;
            default:
                throw new IllegalStateException("Unknown method: + " + method);
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
            case PhoneAuthProvider.PROVIDER_ID:
                return PHONE_IDENTITY;
            // The account type for email/password creds is null
            case EmailAuthProvider.PROVIDER_ID:
            default:
                return null;
        }
    }

    @AuthUI.SupportedProvider
    public static String accountTypeToProviderId(@NonNull String accountType) {
        switch (accountType) {
            case IdentityProviders.GOOGLE:
                return GoogleAuthProvider.PROVIDER_ID;
            case IdentityProviders.FACEBOOK:
                return FacebookAuthProvider.PROVIDER_ID;
            case IdentityProviders.TWITTER:
                return TwitterAuthProvider.PROVIDER_ID;
            case PHONE_IDENTITY:
                return PhoneAuthProvider.PROVIDER_ID;
            default:
                return null;
        }
    }

    @Nullable
    public static AuthUI.IdpConfig getConfigFromIdps(List<AuthUI.IdpConfig> idps, String id) {
        for (AuthUI.IdpConfig idp : idps) {
            if (idp.getProviderId().equals(id)) { return idp; }
        }
        return null;
    }

    @NonNull
    public static AuthUI.IdpConfig getConfigFromIdpsOrThrow(List<AuthUI.IdpConfig> idps,
                                                            String id) {
        AuthUI.IdpConfig config = getConfigFromIdps(idps, id);
        if (config == null) {
            throw new IllegalStateException("Provider " + id + " not found.");
        }
        return config;
    }

    public static Task<String> fetchTopProvider(FirebaseAuth auth, @NonNull String email) {
        if (TextUtils.isEmpty(email)) {
            return Tasks.forException(new NullPointerException("Email cannot be empty"));
        }

        return auth.fetchSignInMethodsForEmail(email)
                .continueWith(new Continuation<SignInMethodQueryResult, String>() {
                    @Override
                    public String then(@NonNull Task<SignInMethodQueryResult> task) {
                        return task.isSuccessful() ? getLastUsedProvider(task.getResult()) : null;
                    }
                }).continueWith(new Continuation<String, String>() {
                    @Override
                    public String then(@NonNull Task<String> task) {
                        String method = task.getResult();
                        if (method == null) {
                            return null;
                        } else {
                            return signInMethodToProviderId(method);
                        }
                    }
                });
    }

    @Nullable
    public static String getLastUsedProvider(SignInMethodQueryResult result) {
        List<String> methods = result.getSignInMethods();
        return methods == null || methods.isEmpty()
                ? null : methods.get(methods.size() - 1);
    }
}
