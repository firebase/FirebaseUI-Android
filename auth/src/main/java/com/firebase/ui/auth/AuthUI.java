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

package com.firebase.ui.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;

import com.firebase.ui.auth.util.CredentialsApiHelper;
import com.firebase.ui.auth.choreographer.idp.provider.FacebookProvider;
import com.firebase.ui.auth.choreographer.idp.provider.GoogleProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.firebase.ui.auth.ui.credentials.ChooseAccountActivity;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The entry point to the user authentication flow.
 *
 * <h1>IDP Provider configuration instructions</h1>
 *
 * <ul>
 *
 * <li>Enabling Google Sign In: If you're using the
 * <a href="https://developers.google.com/android/guides/google-services-plugin">Google
 * Services Gradle Plugin</a>, no additional configuration is required. If not, please override
 * {@code R.string.default_web_client_id} to provide your
 * <a href="https://developers.google.com/identity/sign-in/web/devconsole-project">Google OAuth
 * web client id.</a>
 * </li>
 *
 * <li>Enabling Facebook Sign In: Please override the
 * {@code R.string.facebook_application_id} to provide the
 * <a href="https://developers.facebook.com/docs/apps/register">App Id</a> from
 * <a href="https://developers.facebook.com/apps">Facebook Developer Dashboard</a>.
 * </li>
 *
 * </ul>
 */
public class AuthUI {

    /**
     * Provider identifier for email and password credentials, for use with
     * {@link SignInIntentBuilder#setProviders}.
     */
    public static final String EMAIL_PROVIDER = "email";

    /**
     * Provider identifier for Google, for use with {@link SignInIntentBuilder#setProviders}.
     */
    public static final String GOOGLE_PROVIDER = "google";

    /**
     * Provider identifier for Facebook, for use with {@link SignInIntentBuilder#setProviders}.
     */
    public static final String FACEBOOK_PROVIDER = "facebook";

    /**
     * The set of authentication providers supported in Firebase Auth UI.
     */
    public static final Set<String> SUPPORTED_PROVIDERS =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    EMAIL_PROVIDER,
                    GOOGLE_PROVIDER,
                    FACEBOOK_PROVIDER
            )));

    /**
     * The theme identifier to use in {@link SignInIntentBuilder#setTheme(int)} if no theme
     * customization is required.
     */
    public static final int DEFAULT_THEME = 0;

    /**
     * Signs the current user out, if one is signed in. The
     * {@link FirebaseAuth} instance associated with the default {@link FirebaseApp} instance
     * (as returned by {@code FirebaseApp.getInstance()}) will be used for this operation.
     *
     * @param activity The activity requesting the user be signed out.
     * @return a task which, upon completion, signals that the user has been signed out
     * ({@code result.isSuccess()}, or that the sign-out attempt failed unexpectedly
     * ({@code !result.isSuccess()}).
     */
    public static Task<Void> signOut(@NonNull Activity activity) {
        FirebaseApp firebaseApp = FirebaseApp.getInstance();
        if (firebaseApp == null) {
            throw new IllegalStateException("No FirebaseApp instance available");
        }

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance(firebaseApp);
        if (firebaseAuth == null) {
            throw new IllegalStateException(
                    "No FirebaseAuth instance available for app" + firebaseApp.getName());
        }

        return signOut(activity, firebaseAuth);
    }

    /**
     * Signs the current user out, if one is signed in to the context of the provided
     * {@link FirebaseAuth} instance.
     *
     * @param activity The activity requesting the user be signed out.
     * @param auth The auth instance from which the current user should be signed out.
     * @return a task which, upon completion, signals that the user has been signed out
     * ({@code result.isSuccess()}, or that the sign-out attempt failed unexpectedly
     * ({@code !result.isSuccess()}).
     */
    public static Task<Void> signOut(@NonNull Activity activity, @NonNull FirebaseAuth auth) {
        auth.signOut();
        return CredentialsApiHelper.getInstance(activity).disableAutoSignIn()
                .continueWith(new Continuation<Status, Void>() {
                    @Override
                    public Void then(@NonNull Task<Status> task) throws Exception {
                        return null;
                    }
                });
    }

    /**
     * Builder for the intent to start the user authentication flow.
     */
    public static final class SignInIntentBuilder {
        private Context mContext;
        private FirebaseApp mFirebaseApp;
        private int mTheme = DEFAULT_THEME;
        private List<String> mProviders = Collections.singletonList(EMAIL_PROVIDER);
        private String mTosUrl;

        /**
         * Starts the process of creating a sign in intent, with the mandatory application
         * context parameter.
         */
        public SignInIntentBuilder(@NonNull Context context) {
            setContext(context);
        }

        /**
         * Specifies the context to use in creating the sign-in intent.
         */
        public @NonNull SignInIntentBuilder setContext(@NonNull Context context) {
            if (context == null) {
                throw new IllegalArgumentException("context must not be null");
            }
            mContext = context;
            return this;
        }

        /**
         * Specifies the firebase app to use and update during the authentication flow.
         * If {@code null} is provided, or this method is not called, the value returned by
         * {@link FirebaseApp#getInstance()} will be used.
         */
        public SignInIntentBuilder setFirebaseApp(@Nullable FirebaseApp firebaseApp) {
            if (firebaseApp == null) {
                firebaseApp = getDefaultFirebaseApp();
            }
            mFirebaseApp = firebaseApp;
            return this;
        }

        /**
         * Specifies the theme to use for the application flow. If no theme is specified,
         * {@link #DEFAULT_THEME} will be used.
         */
        public SignInIntentBuilder setTheme(@StyleRes int theme) {
            mTheme = theme;
            return this;
        }

        /**
         * Specifies the terms-of-service URL for the application.
         */
        public SignInIntentBuilder setTosUrl(@Nullable String tosUrl) {
            mTosUrl = tosUrl;
            return this;
        }

        /**
         * Specifies the set of supported authentication providers. At least one provider
         * must be specified, and the set of providers must be a subset of
         * {@link #SUPPORTED_PROVIDERS}.
         *
         * <p>If no providers are explicitly specified by calling this method, then
         * {@link #EMAIL_PROVIDER email} is the default supported provider.
         *
         * @see #EMAIL_PROVIDER
         * @see #FACEBOOK_PROVIDER
         * @see #GOOGLE_PROVIDER
         */
        public SignInIntentBuilder setProviders(@NonNull String... providers) {
            mProviders = Arrays.asList(providers);
            for (String provider : mProviders) {
                if (!SUPPORTED_PROVIDERS.contains(provider)) {
                    throw new IllegalArgumentException("Unknown provider: " + provider);
                }
            }
            return this;
        }

        public Intent build() {
            if (mFirebaseApp == null) {
                mFirebaseApp = getDefaultFirebaseApp();
            }

            ArrayList<IDPProviderParcel> providerParcels = new ArrayList<>();
            for (String provider : mProviders) {
                if (provider.equalsIgnoreCase(FACEBOOK_PROVIDER)) {
                    providerParcels.add(FacebookProvider.createFacebookParcel(
                            mContext.getString(R.string.facebook_application_id)));
                } else if (provider.equalsIgnoreCase(GOOGLE_PROVIDER)) {
                    providerParcels.add(GoogleProvider.createParcel(
                            mContext.getString(R.string.default_web_client_id)));
                } else if (provider.equalsIgnoreCase(EMAIL_PROVIDER)) {
                    providerParcels.add(
                            new IDPProviderParcel(EmailAuthProvider.PROVIDER_ID, new Bundle())
                    );
                }
            }

            return ChooseAccountActivity.createIntent(
                    mContext,
                    mFirebaseApp.getName(),
                    mFirebaseApp.getOptions().getApiKey(),
                    mFirebaseApp.getOptions().getApplicationId(),
                    providerParcels,
                    mTosUrl,
                    mTheme);
        }

        private @NonNull FirebaseApp getDefaultFirebaseApp() {
            FirebaseApp app = FirebaseApp.getInstance();
            if (app == null) {
                throw new IllegalStateException(
                        "no FirebaseApp instance specified or available through "
                                + "FirebaseApp.getInstance()");
            }
            return app;
        }
    }
}
