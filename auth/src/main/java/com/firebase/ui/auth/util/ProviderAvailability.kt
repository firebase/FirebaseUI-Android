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
package com.firebase.ui.auth.util

import androidx.annotation.RestrictTo

/**
 * Utility for checking the availability of authentication providers at runtime.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ProviderAvailability {

    /**
     * Checks if Facebook authentication is available.
     * Returns true if the Facebook SDK is present in the classpath.
     */
    val IS_FACEBOOK_AVAILABLE: Boolean = classExists("com.facebook.login.LoginManager")

    private fun classExists(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}