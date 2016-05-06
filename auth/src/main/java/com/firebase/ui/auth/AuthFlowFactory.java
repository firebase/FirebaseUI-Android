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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.choreographer.idp.provider.FacebookProvider;
import com.firebase.ui.auth.choreographer.idp.provider.GoogleProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.firebase.ui.auth.ui.credentials.CredentialsInitActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class to configure the intent that starts the auth flow
 */
public class AuthFlowFactory {
    /**
     * Creates the intent that starts the auth flow
     * @param context activity context
     * @param appName the app name of the FirebaseApp that you wish to use
     * @param apiaryKey the Firebase backend API key
     * @param applicationId the Firebase application id
     * @param providers the supported identity providers that you wish to enable (google, facebook, etc)
     * @return the Intent to start the auth flow
     */
    public static Intent createIntent(
            @NonNull Context context,
            @NonNull String appName,
            @NonNull String apiaryKey,
            @NonNull String applicationId,
            int theme,
            @Nullable List<String> providers) {
        ArrayList<IDPProviderParcel> providerParcels = new ArrayList<>();
        if (providers == null || providers.size() == 0) {
            return CredentialsInitActivity.createIntent(
                    context,appName, providerParcels, apiaryKey, applicationId, theme);
        }

        for (String provider : providers) {
            if (provider.equalsIgnoreCase("facebook")) {
                providerParcels.add(FacebookProvider.createFacebookParcel(
                        context.getString(R.string.facebook_application_id)));
            } else if (provider.equalsIgnoreCase("google")) {
                providerParcels.add(
                        GoogleProvider.createParcel(context.getString(R.string.google_client_id)));
            }
        }
        return CredentialsInitActivity.createIntent(
                context,appName, providerParcels, apiaryKey, applicationId, theme);
    }
}
