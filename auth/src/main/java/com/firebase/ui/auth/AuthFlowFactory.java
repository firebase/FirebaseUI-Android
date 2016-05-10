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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.choreographer.idp.provider.FacebookProvider;
import com.firebase.ui.auth.choreographer.idp.provider.GoogleProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.firebase.ui.auth.ui.credentials.ChooseAccountActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.EmailAuthProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides the entry point to the user authentication flow.
 */
public class AuthFlowFactory {

    /**
     * Provider identifier for email and password credentials, for use with {@link #createIntent}.
     */
    public static final String EMAIL_PROVIDER = "email";

    /**
     * Provider identifier for Google, for use with {@link #createIntent}.
     */
    public static final String GOOGLE_PROVIDER = "google";

    /**
     * Provider identifier for Facebook, for use with {@link #createIntent}.
     */
    public static final String FACEBOOK_PROVIDER = "facebook";

    /**
     * The theme identifier to use in {@link #createIntent} if no theme customization is required.
     */
    public static final int DEFAULT_THEME = 0;

    /**
     * Creates an intent to start the user authentication flow.
     *
     * <p>IDP Provider instructions:
     *
     * <ul>
     * <li>Enabling Google Sign In: If you're using the
     * <a href="https://developers.google.com/android/guides/google-services-plugin">Google
     * Services Gradle Plugin</a>, no additional configuration is required. If not, please override
     * {@code R.string.default_web_client_id} to provide your
     * <a href="https://developers.google.com/identity/sign-in/web/devconsole-project">Google OAuth
     * web client id.</a>
     * </li>
     * <li>Enabling Facebook Sign In: Please override the
     * {@code R.string.facebook_application_id} to provide the
     * <a href="https://developers.facebook.com/docs/apps/register">App Id</a> from
     * <a href="https://developers.facebook.com/apps">Facebook Developer Dashboard</a>.
     * </li>
     * </ul>
     *
     * @param context The context of the activity that is starting the authentication flow.
     * @param firebaseApp the {@link com.google.firebase.FirebaseApp FirebaseApp} instance to
     *     that the authentication flow should use and update.
     * @param tosUrl the URL of the Term of Service page for your app that should be presented to
     *     the user.
     * @param theme the resource identifier of the customized theme to be applied to the
     *     authentication flow. Use {@link #DEFAULT_THEME} if no customization is required.
     * @param providers the identity providers that you wish to enable (e.g.
     * {@link #GOOGLE google}, {@link #FACEBOOK facebook}, etc).
     * @return An intent to launch the authentication flow.
     */
    public static Intent createIntent(
            @NonNull Context context,
            @NonNull FirebaseApp firebaseApp,
            String tosUrl,
            int theme,
            @Nullable List<String> providers) {
        if (providers == null || providers.size() == 0) {
            providers = Collections.emptyList();
        }

        ArrayList<IDPProviderParcel> providerParcels = new ArrayList<>();
        for (String provider : providers) {
            if (provider.equalsIgnoreCase(FACEBOOK_PROVIDER)) {
                providerParcels.add(FacebookProvider.createFacebookParcel(
                        context.getString(R.string.facebook_application_id)));
            } else if (provider.equalsIgnoreCase(GOOGLE_PROVIDER)) {
                providerParcels.add(GoogleProvider.createParcel(
                        context.getString(R.string.default_web_client_id)));
            } else if (provider.equalsIgnoreCase(EMAIL_PROVIDER)) {
                providerParcels.add(
                        new IDPProviderParcel(EmailAuthProvider.PROVIDER_ID, new Bundle())
                );
            }
        }

        return ChooseAccountActivity.createIntent(
                context,
                firebaseApp.getName(),
                firebaseApp.getOptions().getApiKey(),
                firebaseApp.getOptions().getApplicationId(),
                providerParcels,
                tosUrl,
                theme
        );
    }
}
