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

import android.text.TextUtils;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import static com.firebase.ui.auth.AuthUI.EMAIL_LINK_PROVIDER;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ProviderUtils {
    private static final String GITHUB_IDENTITY = "https://github.com";
    private static final String PHONE_IDENTITY = "https://phone.firebase";

    private ProviderUtils() {
        throw new AssertionError("No instance for you!");
    }

    @Nullable
    public static AuthCredential getAuthCredential(IdpResponse response) {
        if (response.hasCredentialForLinking()) {
            return response.getCredentialForLinking();
        }
        switch (response.getProviderType()) {
            case GoogleAuthProvider.PROVIDER_ID:
                return GoogleAuthProvider.getCredential(response.getIdpToken(), null);
            case FacebookAuthProvider.PROVIDER_ID:
                return FacebookAuthProvider.getCredential(response.getIdpToken());
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
            case GithubAuthProvider.GITHUB_SIGN_IN_METHOD:
                return GithubAuthProvider.PROVIDER_ID;
            case PhoneAuthProvider.PHONE_SIGN_IN_METHOD:
                return PhoneAuthProvider.PROVIDER_ID;
            case EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD:
                return EmailAuthProvider.PROVIDER_ID;
            case EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD:
                return EMAIL_LINK_PROVIDER;
            default:
                return method;
        }
    }

    /**
     * Translate a Firebase Auth provider ID (such as {@link GoogleAuthProvider#PROVIDER_ID}) to a
     * Credentials API account type (such as {@link IdentityProviders#GOOGLE}).
     */
    public static String providerIdToAccountType(
            @AuthUI.SupportedProvider @NonNull String providerId) {
        switch (providerId) {
            case GoogleAuthProvider.PROVIDER_ID:
                return IdentityProviders.GOOGLE;
            case FacebookAuthProvider.PROVIDER_ID:
                return IdentityProviders.FACEBOOK;
            case TwitterAuthProvider.PROVIDER_ID:
                return IdentityProviders.TWITTER;
            case GithubAuthProvider.PROVIDER_ID:
                return GITHUB_IDENTITY;
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
            case GITHUB_IDENTITY:
                return GithubAuthProvider.PROVIDER_ID;
            case PHONE_IDENTITY:
                return PhoneAuthProvider.PROVIDER_ID;
            default:
                return null;
        }
    }

    public static String providerIdToProviderName(@NonNull String providerId) {
        switch (providerId) {
            case GoogleAuthProvider.PROVIDER_ID:
                return AuthUI.getApplicationContext().getString(R.string.fui_idp_name_google);
            case FacebookAuthProvider.PROVIDER_ID:
                return AuthUI.getApplicationContext().getString(R.string.fui_idp_name_facebook);
            case TwitterAuthProvider.PROVIDER_ID:
                return AuthUI.getApplicationContext().getString(R.string.fui_idp_name_twitter);
            case GithubAuthProvider.PROVIDER_ID:
                return AuthUI.getApplicationContext().getString(R.string.fui_idp_name_github);
            case PhoneAuthProvider.PROVIDER_ID:
                return AuthUI.getApplicationContext().getString(R.string.fui_idp_name_phone);
            case EmailAuthProvider.PROVIDER_ID:
            case EMAIL_LINK_PROVIDER:
                return AuthUI.getApplicationContext().getString(R.string.fui_idp_name_email);
            default:
                return null;
        }
    }

    @Nullable
    public static AuthUI.IdpConfig getConfigFromIdps(List<AuthUI.IdpConfig> idps, String id) {
        for (AuthUI.IdpConfig idp : idps) {
            if (idp.getProviderId().equals(id)) {
                return idp;
            }
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

    public static Task<List<String>> fetchSortedProviders(@NonNull FirebaseAuth auth,
                                                          @NonNull final FlowParameters params,
                                                          @NonNull String email) {
        if (TextUtils.isEmpty(email)) {
            return Tasks.forException(new NullPointerException("Email cannot be empty"));
        }

        return auth.fetchSignInMethodsForEmail(email)
                .continueWithTask(new Continuation<SignInMethodQueryResult, Task<List<String>>>() {
                    @Override
                    public Task<List<String>> then(@NonNull Task<SignInMethodQueryResult> task) {
                        List<String> methods = task.getResult().getSignInMethods();
                        if (methods == null) {
                            methods = new ArrayList<>();
                        }

                        List<String> allowedProviders = new ArrayList<>(params.providers.size());
                        for (AuthUI.IdpConfig provider : params.providers) {
                            allowedProviders.add(provider.getProviderId());
                        }

                        List<String> lastSignedInProviders = new ArrayList<>(methods.size());
                        for (String method : methods) {
                            String id = signInMethodToProviderId(method);
                            if (allowedProviders.contains(id)) {
                                lastSignedInProviders.add(0, id);
                            }
                        }

                        // In this case the developer has configured EMAIL_LINK sign in but the
                        // user is a password user. The valid use case here is that the developer
                        // is using admin-created accounts and combining email-link sign in with
                        // setAllowNewAccounts(false). So we manually enable EMAIL_LINK.  See:
                        // https://github.com/firebase/FirebaseUI-Android/issues/1762#issuecomment-661115293
                        if (allowedProviders.contains(EMAIL_LINK_PROVIDER)
                                && methods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)
                                && !methods.contains(EMAIL_LINK_PROVIDER)) {
                            lastSignedInProviders.add(0, signInMethodToProviderId(EMAIL_LINK_PROVIDER));
                        }

                        if (task.isSuccessful() && lastSignedInProviders.isEmpty()
                                && !methods.isEmpty()) {
                            // There is an existing user who only has unsupported sign in methods
                            return Tasks.forException(new FirebaseUiException(ErrorCodes
                                    .DEVELOPER_ERROR));
                        }
                        // Reorder providers from most to least usable. Usability is determined by
                        // how many steps a user needs to perform to log in.
                        reorderPriorities(lastSignedInProviders);

                        return Tasks.forResult(lastSignedInProviders);
                    }

                    private void reorderPriorities(List<String> providers) {
                        // Prioritize Google over everything else
                        // Prioritize email-password sign in second
                        // De-prioritize email link sign in
                        changePriority(providers, EmailAuthProvider.PROVIDER_ID, true);
                        changePriority(providers, GoogleAuthProvider.PROVIDER_ID, true);
                        changePriority(providers, EMAIL_LINK_PROVIDER, false);
                    }

                    private void changePriority(List<String> providers,
                                                String id,
                                                boolean maximizePriority) {
                        if (providers.remove(id)) {
                            if (maximizePriority) {
                                providers.add(0, id);
                            } else {
                                providers.add(id);
                            }
                        }
                    }
                });
    }

    public static Task<String> fetchTopProvider(
            @NonNull FirebaseAuth auth,
            @NonNull FlowParameters params,
            @NonNull String email) {
        return fetchSortedProviders(auth, params, email)
                .continueWithTask(new Continuation<List<String>, Task<String>>() {
                    @Override
                    public Task<String> then(@NonNull Task<List<String>> task) {
                        if (!task.isSuccessful()) {
                            return Tasks.forException(task.getException());
                        }
                        List<String> providers = task.getResult();

                        if (providers.isEmpty()) {
                            return Tasks.forResult(null);
                        } else {
                            return Tasks.forResult(providers.get(0));
                        }
                    }
                });
    }
}
