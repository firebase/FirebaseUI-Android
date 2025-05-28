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

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ProviderAvailability {
    public static final boolean IS_GITHUB_AVAILABLE =
            exists("com.firebase.ui.auth.data.remote.GitHubSignInHandler");
    public static final boolean IS_FACEBOOK_AVAILABLE =
            exists("com.facebook.login.LoginManager");
    public static final boolean IS_TWITTER_AVAILABLE =
            exists("com.twitter.sdk.android.core.identity.TwitterAuthClient");

    private ProviderAvailability() {
        throw new AssertionError("No instance for you!");
    }

    private static boolean exists(String name) {
        boolean exists;
        try {
            Class.forName(name);
            exists = true;
        } catch (ClassNotFoundException e) {
            exists = false;
        }
        return exists;
    }
}
