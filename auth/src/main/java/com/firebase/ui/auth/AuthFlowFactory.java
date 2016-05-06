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
import com.firebase.ui.auth.ui.credentials.CredentialsInitActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.EmailAuthProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class to configure the intent that starts the auth flow
 */
public class AuthFlowFactory {
    public static final String EMAIL_PROVIDER = "email";
    public static final String GOOGLE_PROVIDER = "google";
    public static final String FACEBOOK_PROVIDER = "facebook";

    /**
     * Creates the intent that starts the auth flow
     *
     * IDP Provider instructions:
     * Enabling Google Sign In: If you're using Google Services Gradle Plugin, there are no additional
     *                          steps needed. If not, please override the R.string.default_web_client_id
     *                          to provider your google oauth web client id.
     * Enabling Facebook Sign In: Please override the R.string.facebook_application_id to provide the
     *                          App Id from Facebook Developer Dashboard
     *
     * @param context activity context
     * @param firebaseApp the FirebaseApp that's to used for the authentication flow
     * @param termsOfServiceUrl the URL to the Term of Service page to be present to the user
     * @param theme the customized theme to be applied to the authentication flow. 0 will use the default theme.
    * @param providers the supported identity providers that you wish to enable (google, facebook, etc)
     * @return
     */
    public static Intent createIntent(
//            * @param providers the supported identity providers that you wish to enable (google, facebook, etc)
            @NonNull Context context,
            @NonNull FirebaseApp firebaseApp,
            String termsOfServiceUrl,
            int theme,
            @Nullable List<String> providers) {
        ArrayList<IDPProviderParcel> providerParcels = new ArrayList<>();
        String appName = firebaseApp.getName();
        String apiaryKey = firebaseApp.getOptions().getApiKey();
        String applicationId = firebaseApp.getOptions().getApplicationId();
        if (providers == null || providers.size() == 0) {
            return CredentialsInitActivity.createIntent(
                    context,
                    appName,
                    providerParcels,
                    apiaryKey,
                    applicationId,
                    termsOfServiceUrl,
                    theme
            );
        }

        for (String provider : providers) {
            if (provider.equalsIgnoreCase(FACEBOOK_PROVIDER)) {
                providerParcels.add(FacebookProvider.createFacebookParcel(
                        context.getString(R.string.facebook_application_id)));
            } else if (provider.equalsIgnoreCase(GOOGLE_PROVIDER)) {
                providerParcels.add(
                        GoogleProvider.createParcel(context.getString(R.string.default_web_client_id)));
            } else if (provider.equalsIgnoreCase(EMAIL_PROVIDER)) {
                providerParcels.add(
                        new IDPProviderParcel(EmailAuthProvider.PROVIDER_ID, new Bundle())
                );
            }
        }
        return CredentialsInitActivity.createIntent(
                context,
                appName,
                providerParcels,
                apiaryKey,
                applicationId,
                termsOfServiceUrl,
                theme
        );
    }
}
