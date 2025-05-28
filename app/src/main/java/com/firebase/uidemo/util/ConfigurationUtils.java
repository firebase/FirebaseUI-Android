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

package com.firebase.uidemo.util;

import android.annotation.SuppressLint;
import android.content.Context;

import com.firebase.ui.auth.AuthUI;
import com.firebase.uidemo.R;
import com.google.firebase.auth.ActionCodeSettings;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

@SuppressLint("RestrictedApi")
public final class ConfigurationUtils {

    private ConfigurationUtils() {
        throw new AssertionError("No instance for you!");
    }

    public static boolean isGoogleMisconfigured(@NonNull Context context) {
        return AuthUI.UNCONFIGURED_CONFIG_VALUE.equals(
                context.getString(R.string.default_web_client_id));
    }

    public static boolean isFacebookMisconfigured(@NonNull Context context) {
        return AuthUI.UNCONFIGURED_CONFIG_VALUE.equals(
                context.getString(R.string.facebook_application_id));
    }

    @NonNull
    public static List<AuthUI.IdpConfig> getConfiguredProviders(@NonNull Context context) {
        List<AuthUI.IdpConfig> providers = new ArrayList<>();

        if (!isGoogleMisconfigured(context)) {
            providers.add(new AuthUI.IdpConfig.GoogleBuilder().build());
        }

        if (!isFacebookMisconfigured(context)) {
            providers.add(new AuthUI.IdpConfig.FacebookBuilder().build());
        }

        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                .setAndroidPackageName("com.firebase.uidemo", true, null)
                .setHandleCodeInApp(true)
                .setUrl("https://google.com")
                .build();

        providers.add(new AuthUI.IdpConfig.EmailBuilder()
                .setAllowNewAccounts(true)
                .enableEmailLinkSignIn()
                .setActionCodeSettings(actionCodeSettings)
                .build());

        providers.add(new AuthUI.IdpConfig.TwitterBuilder().build());
        providers.add(new AuthUI.IdpConfig.PhoneBuilder().build());
        providers.add(new AuthUI.IdpConfig.MicrosoftBuilder().build());
        providers.add(new AuthUI.IdpConfig.YahooBuilder().build());
        providers.add(new AuthUI.IdpConfig.AppleBuilder().build());

        return providers;
    }
}
